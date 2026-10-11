package com.cult.cryptids.item;

import com.cult.cryptids.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class M14Item extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private long lastShootGameTime  = -100;
    private long lastReloadGameTime = -100;
    private long lastDrawGameTime   = -100;

    private long aimStartTick = -1L;

    /**
     * 🔫 Длительность выстрела — совпадает с длиной анимации shoot.
     * 0.25 сек × 20 тиков = 5.
     */
    public static final int SHOOT_COOLDOWN = 5;
    public static final int SHOOT_ANIM_TICKS = 5;

    private static final int RELOAD_ANIM_TICKS  = 60;
    private static final int DRAW_ANIM_TICKS    = 20;
    private static final int AIM_TRANSITION_TICKS = 10;

       /** 🕐 Две отдельные карты — клиент и сервер не мешают друг другу. */
    private static final Map<UUID, Long> LAST_SHOT_CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SHOT_SERVER = new ConcurrentHashMap<>();

    private static Map<UUID, Long> cooldownMap(Player player) {
        return player.level().isClientSide() ? LAST_SHOT_CLIENT : LAST_SHOT_SERVER;
    }

    public static boolean isOnShootCooldown(Player player) {
        Long last = cooldownMap(player).get(player.getUUID());
        if (last == null) return false;
        return player.level().getGameTime() - last < SHOOT_COOLDOWN;
    }

    public static void markShotFired(Player player) {
        cooldownMap(player).put(player.getUUID(), player.level().getGameTime());
    }

    private static final double SPEED_IDLE   = 0.33D;
    private static final double SPEED_WALK   = 1.0D;
    private static final double SPEED_SHOOT  = 1.0D;
    private static final double SPEED_RELOAD = 1.0D;
    private static final double SPEED_DRAW   = 1.0D;
    private static final double SPEED_AIM    = 1.0D;

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

    // ==================== ПРИЦЕЛИВАНИЕ ====================

    public void markAimStart(long gameTime) { this.aimStartTick = gameTime; }
    public void markAimEnd() { this.aimStartTick = -1L; }
    public long getAimStartTick() { return aimStartTick; }

    public long getAimElapsed(long now) {
        if (aimStartTick < 0) return 0;
        return now - aimStartTick;
    }

    public boolean isInAimTransition(long now) {
        return aimStartTick >= 0 && getAimElapsed(now) < AIM_TRANSITION_TICKS;
    }

    public void resetAnimation() {
        if (controller != null) controller.forceAnimationReset();
    }

    // ==================== USE (ПКМ) ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift+ПКМ → перезарядка
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

        // ПКМ → прицеливание
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // Клиент увидит isUsingItem()==false и вернётся в idle
    }

    // ==================== ВЫСТРЕЛ ====================

    /**
     * 🔫 Серверная часть выстрела — звук + hitscan.
     * Вызывается из M14ShootPacket.handle().
     */
    public void performShot(ServerLevel level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.M14_SHOOT.get(), SoundSource.PLAYERS, 1.5F, 1.0F);

        doHitscan(level, player);
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

    // ==================== АНИМАЦИИ ====================

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

            boolean aiming = mc.player.isUsingItem()
                    && mc.player.getUseItem().getItem() == this;

            if (aiming) {
                ctrl.setAnimationSpeed(SPEED_AIM);
                if (isInAimTransition(now)) {
                    return state.setAndContinue(RawAnimation.begin().thenPlay("aim"));
                }
                return state.setAndContinue(RawAnimation.begin().thenLoop("aim_idle"));
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