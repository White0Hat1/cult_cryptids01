package com.cult.cryptids.entity;

import com.cult.cryptids.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;

public class SirenHeadEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final Random RNG = new Random();

    // ================= СИНХРОНИЗИРУЕМЫЕ ДАННЫЕ =================
    private static final EntityDataAccessor<Boolean> DATA_GRABBING =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CHASING =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLAMMING =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_REACHING =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.BOOLEAN);

    // ================= ДИНАМИЧЕСКИЙ ХИТБОКС =================
    private static final EntityDimensions NORMAL_DIMENSIONS =
            EntityDimensions.scalable(3.0F, 12.0F);
    private static final EntityDimensions COMPACT_DIMENSIONS =
            EntityDimensions.scalable(1.4F, 8.0F);

    private boolean compactMode = false;
    private int stuckCounter = 0;
    private static final int STUCK_THRESHOLD = 8;

    // ================= 🪓 РАЗРУШЕНИЕ ЛИСТВЫ/БРЁВЕН =================
    // Кулдаун между поломками (тиков)
    private static final int BREAK_COOLDOWN_TICKS = 4;
    private int breakCooldown = 0;
    // Сколько блоков ломать за один «тик разрушения» (в ширину)
    private static final int BREAK_WIDTH = 1;
    // Сколько тиков застрял, чтобы начать ломать
    private static final int BREAK_STUCK_TRIGGER = 6;

    // ================= ЛОКАЛЬНЫЕ ПЕРЕМЕННЫЕ =================
    private LivingEntity caughtEntity = null;
    private int grabTicks = 0;

    // ================= ЗАХВАТ =================
    private static final int LIFT_TICKS = 79;
    private static final int HOLD_TICKS = 40;
    private static final int TOTAL_GRAB_TICKS = LIFT_TICKS + HOLD_TICKS;
    private static final double LIFT_HEIGHT = 8.0D;
    private static final double FORWARD_OFFSET = 4.0D;
    private static final double LERP_FACTOR = 0.35;
    private static final double ORIGINAL_SPEED = 0.25D;

    // ================= УДАР ПО ЗЕМЛЕ =================
    private int slamTicks = 0;
    private boolean slamImpactDone = false;
    private static final int SLAM_IMPACT_TICK = 20;
    private static final int SLAM_TOTAL_TICKS = 40;
    private static final double SLAM_RADIUS = 7.0D;
    private static final float SLAM_DAMAGE = 15.0F;
    private static final double SLAM_KNOCKUP = 1.1D;
    private static final double SLAM_KNOCKBACK_SIDE = 0.6D;

    // ================= ВОЛНА ДЫМА =================
    private static final int DUST_WAVE_TOTAL_TICKS = 60;
    private int dustWaveTicks = 0;
    private boolean dustWaveActive = false;

    // ================= REACH + PULL =================
    private int reachTicks = 0;
    private boolean reachHitDone = false;
    private boolean pullPhase = false;
    private LivingEntity reachTarget = null;
    private static final int REACH_HIT_TICK = 30;
    private static final int PULL_END_TICK = 75;
    private static final int REACH_TOTAL_TICKS = 80;
    private static final double REACH_HIT_RANGE = 7.0D;
    private static final double PULL_DISTANCE = 5.0D;
    private static final double PULL_LERP = 0.08D;
    private static final double PULL_Y_FACTOR = 0.5D;
    private static final double PULL_BOX_SHRINK = 0.08D;

    // ================= 🎵 ЗВУКИ =================
    private int ambientCooldownFar = 0;
    private int ambientCooldownSearch = 0;
    private int ambientCooldownClose = 0;
    private int ambientCooldownChaseFar = 0;
    private int ambientCooldownStatic = 0;
    private int stepCooldown = 0;
    private Vec3 lastPos = Vec3.ZERO;

    private static final double RANGE_FAR_MIN    = 60.0;
    private static final double RANGE_SEARCH_MIN = 30.0;
    private static final double RANGE_CLOSE      = 15.0;

    private static final int CD_FAR         = 20 * 75;
    private static final int CD_SEARCH      = 20 * 50;
    private static final int CD_CLOSE       = 20 * 30;
    private static final int CD_CHASE_FAR   = 20 * 25;
    private static final int CD_STATIC      = 20 * 90;

    private static final int STEP_INTERVAL_WALK = 14;
    private static final int STEP_INTERVAL_RUN  = 9;

    private static final float VOL_AMBIENT       = 3.75F;
    private static final float VOL_AMBIENT_FAR   = 6.0F;
    private static final float VOL_STEP_WALK     = 2.0F;
    private static final float VOL_STEP_RUN      = 2.5F;

    // ================= СКОРОСТИ АНИМАЦИЙ =================
    private static final double SPEED_IDLE        = 0.4D;
    private static final double SPEED_WALK        = 0.6D;
    private static final double SPEED_RUN         = 1.0D;
    private static final double SPEED_ATTACK_GRAB = 0.7D;
    private static final double SPEED_GROUND_SLAM = 1.0D;
    private static final double SPEED_REACH       = 0.75D;

    public SirenHeadEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setMaxUpStep(2.0F);
        this.getNavigation().setCanFloat(true);

        this.setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_OTHER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 0.0F);

        this.ambientCooldownFar = RNG.nextInt(CD_FAR / 2);
        this.ambientCooldownSearch = RNG.nextInt(CD_SEARCH / 2);
        this.ambientCooldownClose = RNG.nextInt(CD_CLOSE / 2);
        this.ambientCooldownChaseFar = RNG.nextInt(CD_CHASE_FAR / 2);
        this.ambientCooldownStatic = RNG.nextInt(CD_STATIC / 2);
    }

    // ================= ДИНАМИЧЕСКИЙ ХИТБОКС =================
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return compactMode ? COMPACT_DIMENSIONS : NORMAL_DIMENSIONS;
    }

    private void updateCompactMode() {
        boolean shouldCompact = compactMode;

        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        double movedSq = dx * dx + dz * dz;

        boolean wantsToMove = this.getTarget() != null || this.getNavigation().isInProgress();

        if (wantsToMove && movedSq < 0.0001D) {
            stuckCounter++;
        } else {
            stuckCounter = Math.max(0, stuckCounter - 2);
        }

        if (stuckCounter >= STUCK_THRESHOLD) {
            shouldCompact = true;
        } else if (stuckCounter <= 0) {
            shouldCompact = false;
        }

        if (shouldCompact != compactMode) {
            compactMode = shouldCompact;
            this.refreshDimensions();
        }
    }

    // ================= 🪓 РАЗРУШЕНИЕ =================
    /**
     * Если Siren упёрся в листву или брёвна в направлении движения —
     * он их ломает, как Wither или Ender Dragon.
     * Работает только на листву и брёвна, не на землю/камень.
     */
    private void tryBreakObstacles() {
        if (breakCooldown > 0) {
            breakCooldown--;
            return;
        }

        // Не ломает во время атак
        if (this.isGrabbing() || this.isSlamming() || this.isReaching()) return;

        // Только если застрял
        if (stuckCounter < BREAK_STUCK_TRIGGER) return;

        // Куда двигаться? Направление на цель, либо look-вектор
        Vec3 dir;
        if (this.getTarget() != null) {
            Vec3 toTarget = this.getTarget().position().subtract(this.position()).normalize();
            dir = toTarget;
        } else {
            dir = Vec3.directionFromRotation(0, this.getYRot());
        }

        // Проверяем блоки в направлении движения
        boolean brokeSomething = false;
        BlockPos origin = this.blockPosition();

        // Ломаем на разной высоте (ноги, грудь, голова)
        int[] heights = {0, 1, 2, 3, 4, 5};

        for (int h : heights) {
            // Проверяем 1 блок прямо перед Siren + по бокам (для ширины)
            for (int w = -BREAK_WIDTH; w <= BREAK_WIDTH; w++) {
                BlockPos checkPos = origin.offset(
                        (int) Math.round(dir.x) + (w == 0 ? 0 : (Math.abs(dir.x) > Math.abs(dir.z) ? 0 : w)),
                        h,
                        (int) Math.round(dir.z) + (w == 0 ? 0 : (Math.abs(dir.z) >= Math.abs(dir.x) ? 0 : w))
                );

                if (tryBreakBlock(checkPos)) {
                    brokeSomething = true;
                }
            }
        }

        // Проверяем блоки прямо на пути Siren (внутри его хитбокса)
        // — чтобы он не «застрял» внутри листвы
        AABB box = this.getBoundingBox().inflate(0.3);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (tryBreakBlock(p.immutable())) {
                brokeSomething = true;
            }
        }

        if (brokeSomething) {
            breakCooldown = BREAK_COOLDOWN_TICKS;
        }
    }

    /**
     * Пытается сломать блок. Возвращает true, если сломал.
     * Ломает только листву и брёвна.
     */
    private boolean tryBreakBlock(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        if (state.isAir()) return false;
        if (state.is(Blocks.BEDROCK)) return false;
        if (state.is(Blocks.OBSIDIAN)) return false;

        // Ломаем ТОЛЬКО листву и брёвна/доски (дерево)
        boolean isLeaves = state.is(BlockTags.LEAVES);
        boolean isLog = state.is(BlockTags.LOGS);
        boolean isPlanks = state.is(BlockTags.PLANKS);
        boolean isWoodenFence = state.is(BlockTags.FENCES) && state.getBlock().getName().getString().toLowerCase().contains("wood");
        boolean isSapling = state.is(BlockTags.SAPLINGS);

        if (!isLeaves && !isLog && !isPlanks && !isWoodenFence && !isSapling) {
            return false;
        }

        // Частицы (до разрушения, чтобы знать текстуру)
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.1);
        }

        // Звук разрушения — берём ванильный звук блока
        this.level().playSound(null, pos, state.getSoundType().getBreakSound(),
                SoundSource.BLOCKS, 1.0F, 0.8F + RNG.nextFloat() * 0.4F);

        // Ломаем без дропа (как Wither)
        this.level().destroyBlock(pos, false, this);

        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GRABBING, false);
        this.entityData.define(DATA_CHASING, false);
        this.entityData.define(DATA_SLAMMING, false);
        this.entityData.define(DATA_REACHING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SirenHeadAttackGrabGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                target -> target instanceof Player p && !p.isCreative() && !p.isSpectator()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            updateCompactMode();
            tryBreakObstacles();

            this.entityData.set(DATA_CHASING, this.getTarget() != null && this.getTarget().isAlive());

            if (this.getTarget() instanceof Player p && (p.isCreative() || p.isSpectator())) {
                this.setTarget(null);
            }

            if (caughtEntity instanceof Player p && (p.isCreative() || p.isSpectator())) {
                releaseCaught();
                caughtEntity = null;
                grabTicks = 0;
                this.entityData.set(DATA_GRABBING, false);
                return;
            }

            tickAmbientSounds();
            tickStepSounds();

            // ================= ЗАХВАТ =================
            if (caughtEntity != null) {
                if (caughtEntity.isAlive() && grabTicks < TOTAL_GRAB_TICKS) {
                    grabTicks++;

                    double heightOffset;
                    if (grabTicks <= LIFT_TICKS) {
                        double progress = (double) grabTicks / LIFT_TICKS;
                        heightOffset = progress * LIFT_HEIGHT;
                    } else {
                        heightOffset = LIFT_HEIGHT;
                    }

                    double yawRad = Math.toRadians(this.getYRot());
                    double dx = -Math.sin(yawRad) * FORWARD_OFFSET;
                    double dz = Math.cos(yawRad) * FORWARD_OFFSET;

                    double targetX = this.getX() + dx;
                    double targetY = this.getY() + heightOffset;
                    double targetZ = this.getZ() + dz;

                    double newX = lerp(caughtEntity.getX(), targetX, LERP_FACTOR);
                    double newY = lerp(caughtEntity.getY(), targetY, LERP_FACTOR);
                    double newZ = lerp(caughtEntity.getZ(), targetZ, LERP_FACTOR);

                    caughtEntity.setDeltaMovement(Vec3.ZERO);
                    caughtEntity.setNoGravity(true);
                    caughtEntity.fallDistance = 0;

                    if (caughtEntity instanceof ServerPlayer sp) {
                        sp.connection.teleport(newX, newY, newZ, sp.getYRot(), sp.getXRot());
                    } else {
                        caughtEntity.setPos(newX, newY, newZ);
                        caughtEntity.hurtMarked = true;
                    }

                    float desiredYaw = (float) (Math.toDegrees(Math.atan2(
                            this.getZ() - caughtEntity.getZ(),
                            this.getX() - caughtEntity.getX()
                    )) - 90F);
                    caughtEntity.setYRot(desiredYaw);
                    caughtEntity.setYHeadRot(desiredYaw);

                    if (grabTicks >= TOTAL_GRAB_TICKS - 1) {
                        releaseCaught();
                        caughtEntity.hurt(this.damageSources().mobAttack(this), 200.0F);
                        caughtEntity = null;
                        grabTicks = 0;
                        this.entityData.set(DATA_GRABBING, false);
                    }
                } else {
                    releaseCaught();
                    caughtEntity = null;
                    grabTicks = 0;
                    this.entityData.set(DATA_GRABBING, false);
                }
            }

            // ================= УДАР ПО ЗЕМЛЕ =================
            if (this.isSlamming()) {
                slamTicks++;

                if (slamTicks == SLAM_IMPACT_TICK && !slamImpactDone) {
                    doGroundSlamImpact();
                    slamImpactDone = true;
                    dustWaveActive = true;
                    dustWaveTicks = 0;
                }

                if (slamTicks >= SLAM_TOTAL_TICKS) {
                    resetSlam();
                }
            }

            // ================= ВОЛНА ДЫМА =================
            if (dustWaveActive) {
                dustWaveTicks++;
                spawnDustWave();

                if (dustWaveTicks >= DUST_WAVE_TOTAL_TICKS) {
                    dustWaveActive = false;
                    dustWaveTicks = 0;
                }
            }

            // ================= REACH + PULL =================
            if (this.isReaching()) {
                handleReachAndPull();
            }
        }
    }

    // ================= 🎵 ЗВУКИ =================
    private void tickAmbientSounds() {
        if (ambientCooldownFar > 0) ambientCooldownFar--;
        if (ambientCooldownSearch > 0) ambientCooldownSearch--;
        if (ambientCooldownClose > 0) ambientCooldownClose--;
        if (ambientCooldownChaseFar > 0) ambientCooldownChaseFar--;
        if (ambientCooldownStatic > 0) ambientCooldownStatic--;

        boolean busy = this.isGrabbing() || this.isSlamming() || this.isReaching();
        if (busy) return;

        if (ambientCooldownStatic == 0) {
            playSirenSound(ModSounds.SIREN_STATIC.get(), VOL_AMBIENT, 0.9F + RNG.nextFloat() * 0.2F);
            ambientCooldownStatic = CD_STATIC + RNG.nextInt(CD_STATIC);
        }

        Player nearest = this.level().getNearestPlayer(this, 128.0D);
        if (nearest == null) return;

        double dist = this.distanceTo(nearest);
        boolean hasTarget = this.getTarget() != null;

        if (hasTarget && dist <= RANGE_CLOSE) {
            if (ambientCooldownClose == 0) {
                playSirenSound(ModSounds.SIREN_CLOSE.get(), VOL_AMBIENT, 1.0F);
                ambientCooldownClose = CD_CLOSE + RNG.nextInt(CD_CLOSE);
            }
            return;
        }

        if (dist > RANGE_CLOSE && dist <= RANGE_FAR_MIN) {
            if (ambientCooldownChaseFar == 0) {
                playSirenSound(ModSounds.SIREN_CHASE_FAR.get(), VOL_AMBIENT, 1.0F);
                ambientCooldownChaseFar = CD_CHASE_FAR + RNG.nextInt(CD_CHASE_FAR);
            } else if (!hasTarget && dist >= RANGE_SEARCH_MIN && ambientCooldownSearch == 0) {
                playSirenSound(ModSounds.SIREN_SEARCH.get(), VOL_AMBIENT, 1.0F);
                ambientCooldownSearch = CD_SEARCH + RNG.nextInt(CD_SEARCH);
            }
            return;
        }

        if (dist > RANGE_FAR_MIN) {
            if (ambientCooldownFar == 0) {
                playSirenSound(ModSounds.SIREN_FAR.get(), VOL_AMBIENT_FAR, 1.0F);
                ambientCooldownFar = CD_FAR + RNG.nextInt(CD_FAR);
            }
        }
    }

    private void tickStepSounds() {
        if (stepCooldown > 0) stepCooldown--;

        boolean busy = this.isGrabbing() || this.isSlamming() || this.isReaching();
        if (busy) return;

        Vec3 cur = this.position();
        double moved = lastPos.distanceToSqr(cur);
        lastPos = cur;

        boolean isMoving = moved > 0.0005D;
        if (!isMoving) return;

        if (stepCooldown == 0) {
            boolean running = this.isChasing();
            SoundEvent step = running ? ModSounds.SIREN_RUN.get() : ModSounds.SIREN_WALK.get();
            int cd = running ? STEP_INTERVAL_RUN : STEP_INTERVAL_WALK;
            float volume = running ? VOL_STEP_RUN : VOL_STEP_WALK;
            float pitch = 0.9F + RNG.nextFloat() * 0.2F;

            playSirenSound(step, volume, pitch);
            stepCooldown = cd;
        }
    }

    private void playSirenSound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                sound, SoundSource.HOSTILE,
                volume, pitch
        );
    }

    // ================= 🖐️ REACH + PULL =================
    private void handleReachAndPull() {
        if (reachTarget == null || !reachTarget.isAlive()) {
            resetReach();
            return;
        }

        reachTicks++;

        if (reachTicks == REACH_HIT_TICK && !reachHitDone) {
            reachHitDone = true;
            double distSq = this.distanceToSqr(reachTarget);
            if (distSq <= REACH_HIT_RANGE * REACH_HIT_RANGE) {
                pullPhase = true;
            }
        }

        if (pullPhase && reachTicks > REACH_HIT_TICK && reachTicks <= PULL_END_TICK) {
            doPullStep();
        }

        if (reachTicks >= PULL_END_TICK && pullPhase) {
            LivingEntity grabbed = reachTarget;
            boolean wasPulled = reachHitDone && pullPhase;
            resetReach();

            if (wasPulled && grabbed != null && grabbed.isAlive()) {
                grabbed.setNoGravity(false);
                grabbed.fallDistance = 0;

                if (RNG.nextBoolean()) {
                    this.startGrabbing(grabbed);
                } else {
                    this.startGroundSlam();
                }
            }
            return;
        }

        if (reachTicks >= REACH_TOTAL_TICKS) {
            resetReach();
        }
    }

    private void doPullStep() {
        double yawRad = Math.toRadians(this.getYRot());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);

        double targetX = this.getX() + fx * PULL_DISTANCE;
        double targetY = this.getY() + 0.1D;
        double targetZ = this.getZ() + fz * PULL_DISTANCE;

        double curX = reachTarget.getX();
        double curY = reachTarget.getY();
        double curZ = reachTarget.getZ();

        double stepX = (targetX - curX) * PULL_LERP;
        double stepY = (targetY - curY) * PULL_LERP * PULL_Y_FACTOR;
        double stepZ = (targetZ - curZ) * PULL_LERP;

        if (Math.abs(stepX) > 0.0005D) {
            double tryX = curX + stepX;
            if (!isBlocked(reachTarget, tryX, curY, curZ)) curX = tryX;
        }
        if (Math.abs(stepZ) > 0.0005D) {
            double tryZ = curZ + stepZ;
            if (!isBlocked(reachTarget, curX, curY, tryZ)) curZ = tryZ;
        }
        if (Math.abs(stepY) > 0.0005D) {
            double tryY = curY + stepY;
            if (!isBlocked(reachTarget, curX, tryY, curZ)) curY = tryY;
        }

        reachTarget.setDeltaMovement(Vec3.ZERO);
        reachTarget.setNoGravity(true);
        reachTarget.fallDistance = 0;

        if (reachTarget instanceof ServerPlayer sp) {
            sp.connection.teleport(curX, curY, curZ, sp.getYRot(), sp.getXRot());
        } else {
            reachTarget.setPos(curX, curY, curZ);
            reachTarget.hurtMarked = true;
        }

        if (this.level() instanceof ServerLevel sl && reachTicks % 4 == 0) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    curX, curY + 0.8, curZ, 2, 0.2, 0.3, 0.2, 0.02);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    curX, curY + 1.2, curZ, 2, 0.15, 0.2, 0.15, 0.015);
        }
    }

    private boolean isBlocked(LivingEntity target, double newX, double newY, double newZ) {
        double dx = newX - target.getX();
        double dy = newY - target.getY();
        double dz = newZ - target.getZ();

        AABB box = target.getBoundingBox().move(dx, dy, dz).deflate(PULL_BOX_SHRINK);
        AABB currentBox = target.getBoundingBox().deflate(PULL_BOX_SHRINK);

        if (!target.level().noCollision(target, currentBox)) return false;
        return !target.level().noCollision(target, box);
    }

    public void startReaching(LivingEntity target) {
        this.reachTicks = 0;
        this.reachHitDone = false;
        this.pullPhase = false;
        this.reachTarget = target;
        this.entityData.set(DATA_REACHING, true);
        this.getNavigation().stop();
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
    }

    private void resetReach() {
        if (reachTarget != null) {
            reachTarget.setNoGravity(false);
            reachTarget.setDeltaMovement(Vec3.ZERO);
            reachTarget.fallDistance = 0;
        }
        this.reachTicks = 0;
        this.reachHitDone = false;
        this.pullPhase = false;
        this.reachTarget = null;
        this.entityData.set(DATA_REACHING, false);
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(ORIGINAL_SPEED);
        }
    }

    public boolean isReaching() {
        return this.entityData.get(DATA_REACHING);
    }

    // ================= ВОЛНА ДЫМА =================
    private void spawnDustWave() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (dustWaveTicks % 12 != 0) return;

        int waveIndex = dustWaveTicks / 12;
        if (waveIndex > 4) return;

        double waveRadius = 1.0D + waveIndex * 1.5D;
        double baseY = this.getY() + 0.15;
        int positionsInRing = 60;

        for (int i = 0; i < positionsInRing; i++) {
            double angle = (Math.PI * 2.0 / positionsInRing) * i + random.nextDouble() * 0.05;
            double radiusJitter = waveRadius + (random.nextDouble() - 0.5) * 0.25;
            double px = this.getX() + Math.cos(angle) * radiusJitter;
            double pz = this.getZ() + Math.sin(angle) * radiusJitter;

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    px, baseY, pz, 2, 0.08, 0.08, 0.08, 0.015);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    px, baseY + 0.05, pz, 1, 0.05, 0.1, 0.05, 0.02);
        }

        if (waveIndex == 0) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), baseY + 0.4, this.getZ(), 20, 0.5, 0.4, 0.5, 0.03);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX(), baseY + 0.5, this.getZ(), 15, 0.4, 0.3, 0.4, 0.03);
        }
    }

    private void doGroundSlamImpact() {
        com.cult.cryptids.network.BloodMoonNetwork.broadcastShake(
                this.getX(), this.getY(), this.getZ(), 32.0D, 20);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class, this.getBoundingBox().inflate(SLAM_RADIUS));

        for (LivingEntity e : targets) {
            if (e == this) continue;
            if (e.isSpectator()) continue;
            if (e instanceof Player p && p.isCreative()) continue;

            e.hurt(this.damageSources().mobAttack(this), SLAM_DAMAGE);

            Vec3 dir = e.position().subtract(this.position());
            double len = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            if (len < 0.01) len = 1.0;
            double nx = dir.x / len;
            double nz = dir.z / len;

            e.push(nx * SLAM_KNOCKBACK_SIDE, SLAM_KNOCKUP, nz * SLAM_KNOCKBACK_SIDE);
            e.hurtMarked = true;
            e.fallDistance = 0;
        }
    }

    private void resetSlam() {
        this.slamTicks = 0;
        this.slamImpactDone = false;
        this.entityData.set(DATA_SLAMMING, false);
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(ORIGINAL_SPEED);
        }
    }

    public void startGroundSlam() {
        this.slamTicks = 0;
        this.slamImpactDone = false;
        this.entityData.set(DATA_SLAMMING, true);
        this.getNavigation().stop();
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
    }

    public boolean isSlamming() {
        return this.entityData.get(DATA_SLAMMING);
    }

    // ================= УТИЛИТЫ =================
    private static double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }

    private void releaseCaught() {
        if (caughtEntity != null) {
            caughtEntity.setNoGravity(false);
            caughtEntity.fallDistance = 0;
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(ORIGINAL_SPEED);
        }
    }

    public void startGrabbing(LivingEntity target) {
        this.caughtEntity = target;
        this.grabTicks = 0;
        this.entityData.set(DATA_GRABBING, true);
        this.getNavigation().stop();
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
    }

    public boolean isGrabbing() {
        return this.entityData.get(DATA_GRABBING);
    }

    public boolean isChasing() {
        return this.entityData.get(DATA_CHASING);
    }

    // ================= АНИМАЦИИ =================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            AnimationController<SirenHeadEntity> ctrl = event.getController();

            if (this.isReaching()) {
                ctrl.setAnimationSpeed(SPEED_REACH);
                return event.setAndContinue(RawAnimation.begin().thenPlay("reach_arm"));
            }
            if (this.isSlamming()) {
                ctrl.setAnimationSpeed(SPEED_GROUND_SLAM);
                return event.setAndContinue(RawAnimation.begin().thenPlay("ground_slam"));
            }
            if (this.isGrabbing()) {
                ctrl.setAnimationSpeed(SPEED_ATTACK_GRAB);
                return event.setAndContinue(RawAnimation.begin().thenPlay("attack_grab"));
            }
            if (event.isMoving() && this.isChasing()) {
                ctrl.setAnimationSpeed(SPEED_RUN);
                return event.setAndContinue(RawAnimation.begin().thenLoop("run"));
            }
            if (event.isMoving()) {
                ctrl.setAnimationSpeed(SPEED_WALK);
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            ctrl.setAnimationSpeed(SPEED_IDLE);
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}