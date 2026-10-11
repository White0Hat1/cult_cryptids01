package com.cult.cryptids.item;

import com.cult.cryptids.ModSounds;
import com.cult.cryptids.client.M14RecoilClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class M14Item extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private long lastShootGameTime  = -100;
    private long lastReloadGameTime = -100;
    private long lastDrawGameTime   = -100;

    private static final int SHOOT_COOLDOWN     = 15;
    private static final int SHOOT_ANIM_TICKS   = 6;
    private static final int RELOAD_ANIM_TICKS  = 60;
    private static final int DRAW_ANIM_TICKS    = 20;

    private static final double SPEED_IDLE   = 0.33D;
    private static final double SPEED_WALK   = 1.0D;
    private static final double SPEED_SHOOT  = 1.0D;
    private static final double SPEED_RELOAD = 1.0D;
    private static final double SPEED_DRAW   = 1.0D;

    private static final double RANGE = 64.0D;
    private static final float DAMAGE = 12.0F;

    private AnimationController<M14Item> controller;

    public M14Item(Properties props) {
        super(props);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public void markShoot(long gameTime)  { this.lastShootGameTime = gameTime; }
    public void markReload(long gameTime) { this.lastReloadGameTime = gameTime; }
    public void markDraw(long gameTime)   { this.lastDrawGameTime = gameTime; }

    public boolean isShooting(long now)  { return (now - lastShootGameTime) < SHOOT_ANIM_TICKS; }
    public boolean isReloading(long now) { return (now - lastReloadGameTime) < RELOAD_ANIM_TICKS; }
    public boolean isDrawing(long now)   { return (now - lastDrawGameTime) < DRAW_ANIM_TICKS; }

    public void resetAnimation() {
        if (controller != null) controller.forceAnimationReset();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.M14_RELOAD.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (level.isClientSide) {
                markReload(level.getGameTime());
                resetAnimation();
            }
            player.getCooldowns().addCooldown(this, RELOAD_ANIM_TICKS);
            return InteractionResultHolder.success(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.M14_SHOOT.get(), SoundSource.PLAYERS, 1.5F, 1.0F);

        if (level.isClientSide) {
            markShoot(level.getGameTime());
            resetAnimation();
            // 🔫 Отдача камеры
            M14RecoilClientState.trigger(10);
        } else {
            doHitscan(level, player);
        }

        player.getCooldowns().addCooldown(this, SHOOT_COOLDOWN);
        return InteractionResultHolder.success(stack);
    }

    private void doHitscan(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));

        double maxDist = blockHit.getType() == HitResult.Type.BLOCK
                ? eye.distanceTo(blockHit.getLocation())
                : RANGE;

        var entities = level.getEntities(player,
                player.getBoundingBox().inflate(RANGE),
                e -> e instanceof LivingEntity && e != player && e.isAlive());

        LivingEntity closest = null;
        double closestDist = maxDist;

        for (var e : entities) {
            if (!(e instanceof LivingEntity le)) continue;
            var box = e.getBoundingBox().inflate(0.3);
            var hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceTo(hit.get());
                if (d < closestDist) {
                    closestDist = d;
                    closest = le;
                }
            }
        }

        if (closest != null) {
            closest.hurt(level.damageSources().playerAttack(player), DAMAGE);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        this.controller = new AnimationController<>(this, "controller", 1, state -> {
            AnimationController<M14Item> ctrl = state.getController();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                ctrl.setAnimationSpeed(SPEED_IDLE);
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }

            long now = mc.level.getGameTime();

            if (isReloading(now)) {
                ctrl.setAnimationSpeed(SPEED_RELOAD);
                return state.setAndContinue(RawAnimation.begin().thenPlay("reload"));
            }
            if (isDrawing(now)) {
                ctrl.setAnimationSpeed(SPEED_DRAW);
                return state.setAndContinue(RawAnimation.begin().thenPlay("draw"));
            }
            if (isShooting(now)) {
                ctrl.setAnimationSpeed(SPEED_SHOOT);
                return state.setAndContinue(RawAnimation.begin().thenPlay("shoot"));
            }

            boolean moving = mc.player.xxa != 0.0F || mc.player.zza != 0.0F
                    || mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.0005;

            if (moving) {
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