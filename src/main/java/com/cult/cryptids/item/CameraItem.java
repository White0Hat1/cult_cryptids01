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
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CameraItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private long lastShootGameTime = -100;
    private long lastInspectGameTime = -100;
    private long lastDrawGameTime = -100;

    private static final int SHOOT_COOLDOWN     = 20;
    private static final int SHOOT_ANIM_TICKS   = 8;
    private static final int INSPECT_ANIM_TICKS = 105;
    private static final int DRAW_ANIM_TICKS    = 33;

    private static final double SPEED_IDLE    = 0.33D;
    private static final double SPEED_WALK    = 1.0D;
    private static final double SPEED_SHOOT   = 1.0D;
    private static final double SPEED_INSPECT = 1.0D;
    private static final double SPEED_DRAW    = 1.0D;

    /** Ссылка на контроллер — для forceAnimationReset(). */
    private AnimationController<CameraItem> controller;

    public CameraItem(Properties props) {
        super(props);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public void markShoot(long gameTime)   { this.lastShootGameTime = gameTime; }
    public void markInspect(long gameTime) { this.lastInspectGameTime = gameTime; }
    public void markDraw(long gameTime)    { this.lastDrawGameTime = gameTime; }

    public boolean isShooting(long now)   { return (now - lastShootGameTime) < SHOOT_ANIM_TICKS; }
    public boolean isInspecting(long now) { return (now - lastInspectGameTime) < INSPECT_ANIM_TICKS; }
    public boolean isDrawing(long now)    { return (now - lastDrawGameTime) < DRAW_ANIM_TICKS; }

    /** 🆕 Публичный сброс анимации — используется при переключении предметов в FP. */
    public void resetAnimation() {
        if (controller != null) {
            controller.forceAnimationReset();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.CAMERA_SHUTTER.get(), SoundSource.PLAYERS, 0.5F, 1.5F);
            if (level.isClientSide) markInspect(level.getGameTime());
            return InteractionResultHolder.success(stack);
        }

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
        this.controller = new AnimationController<>(this, "controller", 1, state -> {
            AnimationController<CameraItem> ctrl = state.getController();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                ctrl.setAnimationSpeed(SPEED_IDLE);
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }

            long now = mc.level.getGameTime();

            if (isDrawing(now)) {
                ctrl.setAnimationSpeed(SPEED_DRAW);
                return state.setAndContinue(RawAnimation.begin().thenPlay("draw"));
            }

            if (isShooting(now)) {
                ctrl.setAnimationSpeed(SPEED_SHOOT);
                return state.setAndContinue(RawAnimation.begin().thenPlay("shoot"));
            }

            if (isInspecting(now)) {
                ctrl.setAnimationSpeed(SPEED_INSPECT);
                return state.setAndContinue(RawAnimation.begin().thenPlay("inspect"));
            }

            boolean pressingMove = mc.player.xxa != 0.0F
                    || mc.player.zza != 0.0F
                    || mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.0005;

            if (pressingMove) {
                ctrl.setAnimationSpeed(SPEED_WALK);
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }

            ctrl.setAnimationSpeed(SPEED_IDLE);
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        });

        controllers.add(this.controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}