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

    private static final double CHASE_SPEED = 1.8D;
    private static final double CRAWL_FORCED_SPEED = 2.0D;

    /** 🥷 Скорость скрытного передвижения. */
    private static final double STALK_SPEED = 1.0D;

    /** 🥷 На каком расстоянии за спиной цели держаться. */
    private static final double STALK_BEHIND_DISTANCE = 2.5D;

    /** 🥷 Если Siren ближе этой дистанции к позиции за спиной — стоит. */
    private static final double STALK_HOLD_DISTANCE_SQ = 1.5D;

    /** ⏱️ Как часто пересчитывать путь в стелсе (в тиках). */
    private static final int STALK_PATH_RECALC_INTERVAL = 15;

    private static final double ATTACK_DISTANCE_SQ = 25.0D;
    private static final double REACH_TRIGGER_DISTANCE_SQ = 64.0D;
    private static final int STUCK_TICKS_THRESHOLD = 20;

    private Vec3 lastPathTarget = null;
    private int lastPathTick = -100;
    private int stuckTicks = 0;

    /** 🥷 Тик последнего пересчёта пути в стелсе. */
    private int lastStalkPathTick = -100;
    /** 🥷 Последняя цель, к которой строили путь в стелсе. */
    private Vec3 lastStalkTarget = null;

    private static final Random RNG = new Random();

    public SirenHeadAttackGrabGoal(SirenHeadEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private double maxChaseDistSq() {
        double range = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return range * range;
    }

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
        this.lastStalkPathTick = -100;
        this.lastStalkTarget = null;

        if (!this.mob.isStalking()) {
            this.mob.getNavigation().moveTo(this.target, getChaseSpeed());
        }
    }

    @Override
    public void tick() {
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        // 🥷 Стелс-режим
        if (this.mob.isStalking()) {
            tickStalkMovement();
            return;
        }

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

    /**
     * 🥷 Скрытное передвижение — Siren идёт к позиции ЗА СПИНОЙ игрока.
     *
     * ⚠️ Путь пересчитывается раз в STALK_PATH_RECALC_INTERVAL тиков,
     *    а не каждый тик — иначе A* съедает весь TPS.
     */
    private void tickStalkMovement() {
        if (this.target == null) return;

        Vec3 look = this.target.getLookAngle();
        double behindX = this.target.getX() - look.x * STALK_BEHIND_DISTANCE;
        double behindY = this.target.getY();
        double behindZ = this.target.getZ() - look.z * STALK_BEHIND_DISTANCE;

        double distToBehindSq = this.mob.distanceToSqr(behindX, behindY, behindZ);

        // Уже на позиции за спиной — стоим
        if (distToBehindSq <= STALK_HOLD_DISTANCE_SQ) {
            this.mob.getNavigation().stop();
            this.lastStalkTarget = null;
            return;
        }

        // ⏱️ Пересчитываем путь только раз в N тиков или если цель сдвинулась больше 2 блоков
        boolean shouldUpdate = false;

        if (this.mob.tickCount - this.lastStalkPathTick >= STALK_PATH_RECALC_INTERVAL) {
            shouldUpdate = true;
        }

        if (this.lastStalkTarget == null
                || this.lastStalkTarget.distanceToSqr(behindX, behindY, behindZ) > 4.0D) {
            shouldUpdate = true;
        }

        if (this.mob.getNavigation().isDone()) {
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            this.mob.getNavigation().moveTo(behindX, behindY, behindZ, STALK_SPEED);
            this.lastStalkTarget = new Vec3(behindX, behindY, behindZ);
            this.lastStalkPathTick = this.mob.tickCount;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) return false;
        if (this.mob.isGrabbing() || this.mob.isSlamming() || this.mob.isReaching()) return true;
        if (this.mob.isForceCrawling()) return true;

        if (this.mob.isStalking()) return true;

        double dx = this.mob.getX() - this.target.getX();
        double dz = this.mob.getZ() - this.target.getZ();
        double horizontalDistSq = dx * dx + dz * dz;
        return horizontalDistSq < maxChaseDistSq();
    }

    @Override
    public void stop() {
        this.target = null;
        this.lastPathTarget = null;
        this.lastStalkTarget = null;
        this.stuckTicks = 0;
        this.mob.getNavigation().stop();
    }
}