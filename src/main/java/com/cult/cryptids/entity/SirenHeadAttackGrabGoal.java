package com.cult.cryptids.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Random;

public class SirenHeadAttackGrabGoal extends Goal {
    private final SirenHeadEntity mob;
    private LivingEntity target;

    // 🏃 Скорость погони ≈ как у игрока в спринте + прыжки.
    private static final double CHASE_SPEED = 1.8D;

    /** 🐾 Скорость на четвереньках — форсированное ползание после долгой погони. */
    private static final double CRAWL_FORCED_SPEED = 2.0D;

    // 📏 Радиус обычной атаки — 5 блоков (squared = 25)
    private static final double ATTACK_DISTANCE_SQ = 25.0D;

    // 🖐️ Радиус, в котором включается «тянущаяся рука» (8 блоков)
    private static final double REACH_TRIGGER_DISTANCE_SQ = 64.0D;

    // ⏱️ Сколько тиков без пути, чтобы начать тянуться
    private static final int STUCK_TICKS_THRESHOLD = 20;

    private Vec3 lastPathTarget = null;
    private int lastPathTick = -100;
    private int stuckTicks = 0;

    private static final Random RNG = new Random();

    public SirenHeadAttackGrabGoal(SirenHeadEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /** Радиус погони = FOLLOW_RANGE моба (квадрат). */
    private double maxChaseDistSq() {
        double range = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return range * range;
    }

    /** 🐾 Скорость с учётом форсированного ползания. */
    private double getChaseSpeed() {
        if (this.mob.isForceCrawling()) return CRAWL_FORCED_SPEED;
        return CHASE_SPEED;
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null || !livingentity.isAlive()) return false;

        double dx = this.mob.getX() - livingentity.getX();
        double dz = this.mob.getZ() - livingentity.getZ();
        if (dx * dx + dz * dz > maxChaseDistSq()) return false;

        this.target = livingentity;
        return true;
    }

    @Override
    public void start() {
        this.lastPathTarget = null;
        this.lastPathTick = -100;
        this.stuckTicks = 0;
        this.mob.getNavigation().moveTo(this.target, getChaseSpeed());
    }

    @Override
    public void tick() {
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (mob.isGrabbing() || mob.isSlamming() || mob.isReaching() || mob.isWallClimbing()) return;

        double dx = this.mob.getX() - this.target.getX();
        double dz = this.mob.getZ() - this.target.getZ();
        double horizontalDistSq = dx * dx + dz * dz;

        boolean hasSight = this.mob.getSensing().hasLineOfSight(this.target);

        Path testPath = this.mob.getNavigation().createPath(this.target, 0);
        boolean canReach = testPath != null && testPath.canReach();

        if (horizontalDistSq <= ATTACK_DISTANCE_SQ && hasSight && canReach) {
            this.mob.getNavigation().stop();
            if (RNG.nextBoolean()) {
                this.mob.startGrabbing(this.target);
            } else {
                this.mob.startGroundSlam();
            }
            return;
        }

        boolean stuck = this.mob.getNavigation().isDone()
                || !canReach
                || (horizontalDistSq <= ATTACK_DISTANCE_SQ && !hasSight);

        if (stuck && horizontalDistSq <= REACH_TRIGGER_DISTANCE_SQ) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }

        if (stuckTicks >= STUCK_TICKS_THRESHOLD) {
            this.mob.getNavigation().stop();
            this.mob.startReaching(this.target);
            stuckTicks = 0;
            return;
        }

        boolean shouldUpdatePath = false;

        if (this.mob.tickCount - this.lastPathTick >= 20) {
            if (this.lastPathTarget == null
                    || this.lastPathTarget.distanceToSqr(
                    this.target.getX(), this.target.getY(), this.target.getZ()) > 1.0D) {
                shouldUpdatePath = true;
            }
        }

        if (this.mob.getNavigation().isDone()) {
            shouldUpdatePath = true;
        }

        if (shouldUpdatePath) {
            this.mob.getNavigation().moveTo(this.target, getChaseSpeed());
            this.lastPathTarget = new Vec3(this.target.getX(), this.target.getY(), this.target.getZ());
            this.lastPathTick = this.mob.tickCount;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) return false;
        if (this.mob.isGrabbing() || this.mob.isSlamming() || this.mob.isReaching()) return true;

        // 🐾 Форсированное ползание = не сдаётся на дистанции
        if (this.mob.isForceCrawling()) return true;

        double dx = this.mob.getX() - this.target.getX();
        double dz = this.mob.getZ() - this.target.getZ();
        double horizontalDistSq = dx * dx + dz * dz;
        return horizontalDistSq < maxChaseDistSq();
    }

    @Override
    public void stop() {
        this.target = null;
        this.lastPathTarget = null;
        this.stuckTicks = 0;
        this.mob.getNavigation().stop();
    }
}