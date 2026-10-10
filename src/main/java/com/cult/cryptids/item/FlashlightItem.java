package com.cult.cryptids.item;

import com.cult.cryptids.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
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

public class FlashlightItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String NBT_ON = "isOn";

    private long lastToggleGameTime = -100;
    private long lastDrawGameTime = -100;

    private static final int TOGGLE_ANIM_TICKS = 10;
    private static final int DRAW_ANIM_TICKS   = 20;

    private static final double SPEED_IDLE   = 0.33D;
    private static final double SPEED_WALK   = 1.0D;
    private static final double SPEED_TOGGLE = 1.0D;
    private static final double SPEED_DRAW   = 1.0D;

    private static final float TOGGLE_VOLUME = 0.7F;
    private static final float PITCH_ON  = 1.15F;
    private static final float PITCH_OFF = 0.85F;

    /** Ссылка на контроллер — нужна для forceAnimationReset(). */
    private AnimationController<FlashlightItem> controller;

    public FlashlightItem(Properties props) {
        super(props);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // ==================== NBT ====================

    public static boolean isOn(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(NBT_ON);
    }

    public static void setOn(ItemStack stack, boolean on) {
        stack.getOrCreateTag().putBoolean(NBT_ON, on);
    }

    public static void toggle(ItemStack stack) {
        setOn(stack, !isOn(stack));
    }

    // ==================== ТАЙМИНГИ ====================

    public void markToggle(long gameTime) { this.lastToggleGameTime = gameTime; }
    public void markDraw(long gameTime)   { this.lastDrawGameTime   = gameTime; }

    public boolean isToggling(long now) { return (now - lastToggleGameTime) < TOGGLE_ANIM_TICKS; }
    public boolean isDrawing(long now)  { return (now - lastDrawGameTime)   < DRAW_ANIM_TICKS; }

    /** 🆕 Публичный сброс анимации — используется при переключении предметов в FP. */
    public void resetAnimation() {
        if (controller != null) {
            controller.forceAnimationReset();
        }
    }

    // ==================== USE (ПКМ) ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean willBeOn = !isOn(stack);
        toggle(stack);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.FLASHLIGHT_TOGGLE.get(), SoundSource.PLAYERS,
                TOGGLE_VOLUME, willBeOn ? PITCH_ON : PITCH_OFF);

        if (level.isClientSide) {
            markToggle(level.getGameTime());
            resetAnimation();
        }

        return InteractionResultHolder.success(stack);
    }

    // ==================== АНИМАЦИИ ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        this.controller = new AnimationController<>(this, "controller", 1, state -> {
            AnimationController<FlashlightItem> ctrl = state.getController();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                ctrl.setAnimationSpeed(SPEED_IDLE);
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }

            long now = mc.level.getGameTime();

            if (isToggling(now)) {
                ctrl.setAnimationSpeed(SPEED_TOGGLE);
                return state.setAndContinue(RawAnimation.begin().thenPlay("toggle"));
            }

            if (isDrawing(now)) {
                ctrl.setAnimationSpeed(SPEED_DRAW);
                return state.setAndContinue(RawAnimation.begin().thenPlay("draw"));
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