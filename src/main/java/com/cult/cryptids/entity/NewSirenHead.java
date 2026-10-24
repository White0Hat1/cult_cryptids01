package com.cult.cryptids.entity;

import com.cult.cryptids.client.CameraOffsetManager;
import com.cult.cryptids.client.ClientUtil;
import com.cult.cryptids.reg.ModDamages;
import com.cult.cryptids.reg.ModSounds;
import com.cult.cryptids.entity.navigation.SmartClimberNavigation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class NewSirenHead extends Monster implements WallClimber, GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING_ID = SynchedEntityData.defineId(NewSirenHead.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Ability> DATA_ABILITY_ID = SynchedEntityData.defineId(NewSirenHead.class, Ability.SERIALIZER);
    private static final EntityDimensions CRAWL_DIMENSIONS = EntityDimensions.scalable(2.98F, 5.0F);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected boolean tight;
    protected int abilityTick;
    private boolean playsChase;

    public NewSirenHead(EntityType<? extends NewSirenHead> type, Level lvl) {
        super(type, lvl);
        moveControl = new SirenHeadMoveControl();
        lookControl = new SirenHeadLookControl();
        setMaxUpStep(2);
        xpReward = 0;
    }

    // Тема погони и движение игрока на клиенте
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (playsChase ^ isAggressive() && (playsChase = !playsChase)) ClientUtil.sirenChase(this);
            if (abilityTick > 0 && getAbility() == Ability.GRAB) {
                Player player = Objects.requireNonNull((Player) Minecraft.getInstance().player); // Без конвертации в не клиентский класс Player сервер слетает
                player.setDeltaMovement(getGrabPos().subtract(player.getBoundingBox().getCenter()));
                if (++abilityTick == 60) abilityTick = 0;
            }
        }
    }

    // Смена хитбокса при ползании
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isSprinting() ? CRAWL_DIMENSIONS : super.getDimensions(pose);
    }


    // Методы движения
    // Лазание
    @Override
    public void travel(Vec3 vec3) {
        if (horizontalCollision && isClimbing()) {
            setDeltaMovement(0, 0.3, 0);
            move(MoverType.SELF, getDeltaMovement());
        }
        super.travel(vec3);
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    // Не боится прыгать с высоты
    @Override
    public int getMaxFallDistance() {
        return Integer.MAX_VALUE;
    }


    // Звуки
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound((isAggressive() ? ModSounds.SIREN_RUN : ModSounds.SIREN_WALK).get(), 1, getVoicePitch());
        CameraOffsetManager.screenShake(4.5f, 5);
    }


    // Поведение
    @Override
    protected void customServerAiStep() {
        if (isSprinting()) {
            if (isClimbing() || getAbility() != Ability.NONE) setSprinting(false);
            else if (!tight) {
                Entity target = getTarget();
                if (target == null || distanceToSqr(target) < 256) setSprinting(false);
            }
        } else if (!isClimbing()) {
            if (tight) setSprinting(true);
            else if (getAbility() == Ability.NONE) {
                Entity target = getTarget();
                if (target != null && distanceToSqr(target) > 1024) setSprinting(true);
            }
        }
        tickAbilities();
        super.customServerAiStep();
    }

    public void tickAbilities() {
        switch (getAbility()) {
            case GRAB -> {
                LivingEntity target = getTarget();
                if (++abilityTick == 60) {
                    if (target != null) target.hurt(ModDamages.cryptid(this), 200);
                    setAbility(Ability.NONE);
                } else if (target != null) {
                    Vec3 vec3 = getGrabPos();
                    if (abilityTick == 1) {
                        target.setPos(vec3);
                        // Отправляем сообщение игроку, чтобы не приходилось постоянно слать новую позицию
                        if (target instanceof ServerPlayer serverPlayer) serverPlayer.connection.send(new ClientboundEntityEventPacket(this, (byte) 62));
                    }
                    else target.setDeltaMovement(vec3.subtract(target.getBoundingBox().getCenter()));
                }
            }

            case SLAM -> {
                if (++abilityTick == 16) {
                    level().getEntities(NewSirenHead.this, getBoundingBox().inflate(6, 0, 6),
                                    Entity::isAttackable).forEach(entity -> entity.hurt(ModDamages.cryptid(this), 40 / distanceTo(entity)));
                    level().broadcastEntityEvent(NewSirenHead.this, (byte) 61);
                } else if (abilityTick == 40) setAbility(Ability.NONE);
            }
        }
    }

    private Vec3 getGrabPos() {
        float rot = getYRot() * Mth.DEG_TO_RAD;
        return position().add(-5 * Math.sin(rot), 2 + abilityTick * 0.134, 5 * Math.cos(rot));
    }

    @Override
    protected PathNavigation createNavigation(Level lvl) {
        return new SmartClimberNavigation(this, lvl, CRAWL_DIMENSIONS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SirenHeadSlamGoal());
        goalSelector.addGoal(3, new SirenHeadGrabGoal());
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    // Синхронные данные
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_CLIMBING_ID, false);
        entityData.define(DATA_ABILITY_ID, Ability.NONE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (data == DATA_SHARED_FLAGS_ID) refreshDimensions();
    }

    public boolean isClimbing() {
        return entityData.get(DATA_CLIMBING_ID);
    }

    public Ability getAbility() {
        return entityData.get(DATA_ABILITY_ID);
    }

    public void setAbility(Ability ability) {
        entityData.set(DATA_ABILITY_ID, ability);
        abilityTick = 0;
    }

    public void setClimbing(boolean climbing) {
        entityData.set(DATA_CLIMBING_ID, climbing);
    }


    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 61 -> { // Эффекты при ударе об землю
                CameraOffsetManager.screenShake(30, 20);
                for (double dist = 1.5; dist < 7; dist += 1.5)
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 11.99) {
                        Vec3 vec3 = position().add(dist * Math.sin(angle), 0, dist * Math.cos(angle));
                        BlockPos.MutableBlockPos pos = BlockPos.containing(vec3).mutable();
                        Level lvl = level();
                        while (lvl.getBlockState(pos).isCollisionShapeFullBlock(lvl, pos)) pos.move(0, 1, 0);
                        while (!lvl.getBlockState(pos).isCollisionShapeFullBlock(lvl, pos)) pos.move(0, -1, 0);
                        lvl.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, lvl.getBlockState(pos)),
                                vec3.x, pos.getY() + 1, vec3.z, 0, dist * 0.3, 0);
                    }
            }

            case 62 -> { // Отправляется игроку, которого схватили
                Objects.requireNonNull(Minecraft.getInstance().player).setPos(getGrabPos());
                abilityTick = 1;
            }
            default -> super.handleEntityEvent(id);
        }
    }

    public boolean playsChase() {
        return playsChase;
    }

    // Geckolib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "move", 5, event -> {
            AnimationController<NewSirenHead> ctrl = event.getController();
            if (isClimbing()) {
                ctrl.setAnimationSpeed(0.9);
                return event.setAndContinue(RawAnimation.begin().thenLoop("wall_climb"));
            } else if (isSprinting()) {
                if (event.isMoving()) {
                    ctrl.setAnimationSpeed(1.4);
                    return event.setAndContinue(RawAnimation.begin().thenLoop("crawl_walk"));
                }
                ctrl.setAnimationSpeed(0.7);
                return event.setAndContinue(RawAnimation.begin().thenLoop("crawl_idle"));
            } else if (event.isMoving()) {
                if (isAggressive()) {
                    ctrl.setAnimationSpeed(1);
                    return event.setAndContinue(RawAnimation.begin().thenLoop("run"));
                }
                ctrl.setAnimationSpeed(0.6);
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            ctrl.setAnimationSpeed(0.4);
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }), new AnimationController<>(this, "attack", 5, event -> switch (getAbility()) {
            case GRAB -> event.setAndContinue(RawAnimation.begin().thenLoop("attack_grab"));
            case SLAM -> event.setAndContinue(RawAnimation.begin().thenLoop("ground_slam"));
            default -> PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }


    // Классы Сиреноголового
    public enum Ability {
        NONE, GRAB, SLAM;

        public static final EntityDataSerializer<Ability> SERIALIZER = EntityDataSerializer.simpleEnum(Ability.class);

        static {
            EntityDataSerializers.registerSerializer(SERIALIZER);
        }
    }

    protected class SirenHeadGrabGoal extends Goal {
        private LivingEntity target;
        private double pathedTargetX;
        private double pathedTargetY;
        private double pathedTargetZ;
        private int ticksUntilNextPathRecalculation;
        private long lastCanUseCheck;

        public SirenHeadGrabGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (tickCount - lastCanUseCheck < 20) return false;
            lastCanUseCheck = tickCount;
            if (getAbility() != Ability.NONE) return false;
            target = getTarget();
            return target != null && target.isAlive() && getNavigation().moveTo(target, 1.8);
        }

        @Override
        public void start() {
            setAggressive(true);
            ticksUntilNextPathRecalculation = 0;
        }

        public boolean canContinueToUse() {
            return target.isAlive() && isWithinRestriction(target.blockPosition()) &&
                    !(target instanceof Player player && (player.isSpectator() || player.isCreative()));
        }

        @Override
        public void stop() {
            setAggressive(false);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (getAbility() != Ability.NONE) return;
            getLookControl().setLookAt(target, 30, 30);
            double d = getPerceivedTargetDistanceSquareForMeleeAttack(target);
            ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);
            if (ticksUntilNextPathRecalculation == 0 && (pathedTargetX == 0 && pathedTargetY == 0 && pathedTargetZ == 0 ||
                    target.distanceToSqr(pathedTargetX, pathedTargetY, pathedTargetZ) >= 1 || getRandom().nextFloat() < 0.05f)) {
                pathedTargetX = target.getX();
                pathedTargetY = target.getY();
                pathedTargetZ = target.getZ();
                ticksUntilNextPathRecalculation = 4 + getRandom().nextInt(7);
                if (d > 1024) ticksUntilNextPathRecalculation += 10;
                else if (d > 256) ticksUntilNextPathRecalculation += 5;
                if (!getNavigation().moveTo(target, 1.8)) ticksUntilNextPathRecalculation += 15;
                ticksUntilNextPathRecalculation = adjustedTickDelay(ticksUntilNextPathRecalculation);
            }

            Vec3 vec3 = target.getBoundingBox().getCenter().subtract(position());
            if (vec3.y >= -target.getBbHeight() && vec3.y <= getBbHeight() && vec3.horizontalDistanceSqr() <= 25) {
                getNavigation().stop();
                lookAt(target, 180, 90);
                yHeadRot = yBodyRot = getYRot();
                setAbility(Ability.GRAB);
            }
        }
    }

    protected class SirenHeadSlamGoal extends Goal {
        private int checkTicks;

        @Override
        public boolean canUse() {
            if (tickCount - checkTicks < 40) return false;
            checkTicks = tickCount;
            return getAbility() == Ability.NONE && !isSprinting() && level().getEntities(NewSirenHead.this,
                    getBoundingBox().inflate(6, 0, 6),
                    entity -> entity.isAttackable() && !(entity instanceof Enemy)).size() > 1;
        }

        @Override
        public void start() {
            getNavigation().stop();
            setAbility(Ability.SLAM);
        }

        @Override
        public boolean canContinueToUse() {
            return getAbility() == Ability.SLAM;
        }

        @Override
        public void stop() {
            setAbility(Ability.NONE);
        }
    }

    protected class SirenHeadMoveControl extends MoveControl {
        protected boolean canWalk = true;

        public SirenHeadMoveControl() {
            super(NewSirenHead.this);
        }

        @Override
        public void tick() {
            if (operation == Operation.MOVE_TO) {
                boolean willWalk = mob.level().noCollision(new AABB(-1.49, 5, -1.49,
                        1.49, 12, 1.49).move(wantedX, wantedY, wantedZ));
                tight = !(canWalk && willWalk);
                canWalk = willWalk;
            }
            super.tick();
        }
    }

    private class SirenHeadLookControl extends LookControl {
        public SirenHeadLookControl() {
            super(NewSirenHead.this);
        }

        @Override
        public void tick() {
            if (getAbility() != Ability.GRAB) super.tick();
        }
    }
}
