package com.cult.cryptids.item;

import com.cult.cryptids.ModSounds;
import com.cult.cryptids.client.CameraHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CameraItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Тайминги
    private long lastShootGameTime = -100;
    private long lastInspectGameTime = -100;

    private static final int SHOOT_COOLDOWN = 20;
    private static final int SHOOT_ANIM_TICKS = 8;
    private static final int INSPECT_ANIM_TICKS = 50;

    // Скорости анимаций (1.0 = норма, 0.33 = в 3 раза медленнее)
    private static final double SPEED_IDLE    = 0.33D;
    private static final double SPEED_WALK    = 1.0D;
    private static final double SPEED_SHOOT   = 1.0D;
    private static final double SPEED_INSPECT = 1.0D;

    public CameraItem(Properties props) {
        super(props);
    }

    public void markShoot(long gameTime) {
        this.lastShootGameTime = gameTime;
    }

    public void markInspect(long gameTime) {
        this.lastInspectGameTime = gameTime;
    }

    public boolean isShooting(long now) {
        return (now - lastShootGameTime) < SHOOT_ANIM_TICKS;
    }

    public boolean isInspecting(long now) {
        return (now - lastInspectGameTime) < INSPECT_ANIM_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift + ПКМ = осмотр
        if (player.isShiftKeyDown()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.CAMERA_SHUTTER.get(), SoundSource.PLAYERS, 0.5F, 1.5F);
            if (level.isClientSide) {
                markInspect(level.getGameTime());
            }
            return InteractionResultHolder.success(stack);
        }

        // Обычный ПКМ = снимок
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CAMERA_SHUTTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (level.isClientSide) {
            CameraHelper.doPhoto(player);
            markShoot(level.getGameTime());
        }

        player.getCooldowns().addCooldown(this, SHOOT_COOLDOWN);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // ⚠️ Transition time = 1 тик → мгновенный переход walk → idle
        controllers.add(new AnimationController<>(this, "controller", 1, state -> {
            AnimationController<CameraItem> ctrl = state.getController();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                ctrl.setAnimationSpeed(SPEED_IDLE);
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }

            long now = mc.level.getGameTime();

            // 1. Снимок (приоритет 1)
            if (isShooting(now)) {
                ctrl.setAnimationSpeed(SPEED_SHOOT);
                return state.setAndContinue(RawAnimation.begin().thenPlay("shoot"));
            }

            // 2. Осмотр (приоритет 2)
            if (isInspecting(now)) {
                ctrl.setAnimationSpeed(SPEED_INSPECT);
                return state.setAndContinue(RawAnimation.begin().thenPlay("inspect"));
            }

            // 3. Ходьба (приоритет 3)
            if (mc.player.walkAnimation.isMoving()) {
                ctrl.setAnimationSpeed(SPEED_WALK);
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }

            // 4. Idle — замедлен в 3 раза
            ctrl.setAnimationSpeed(SPEED_IDLE);
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}