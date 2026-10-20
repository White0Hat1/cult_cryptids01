package com.cult.cryptids.entity.navigation;

import com.cult.cryptids.entity.WallClimber;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SmartClimberNavigation extends GroundPathNavigation {
    protected final WallClimber wallClimber;
    protected Vec3 climbPos;

    public <T extends Mob & WallClimber> SmartClimberNavigation(T mob, Level lvl) {
        super(mob, lvl);
        wallClimber = mob;
    }

    public <T extends Mob & WallClimber> SmartClimberNavigation(T mob, Level lvl, EntityDimensions minHitbox, EntityDimensions maxHitbox) {
        this(mob, lvl);
        ((SmartClimberNodeEvaluator) nodeEvaluator).shiftsHitbox(minHitbox, maxHitbox);
    }

    public <T extends Mob & WallClimber> SmartClimberNavigation(T mob, Level level, EntityDimensions minHitbox) {
        this(mob, level, minHitbox, mob.getType().getDimensions());
    }

    @Override
    protected @Nonnull PathFinder createPathFinder(int maxNodes) {
        nodeEvaluator = new SmartClimberNodeEvaluator();
        return new PathFinder(nodeEvaluator, maxNodes);
    }

    @Override
    public void tick() {
        super.tick();
        if (wallClimber.isClimbing()) {
            if (path == null || path.isDone() || mob.getY() >= path.getNextNode().y) wallClimber.setClimbing(false);
            else if (climbPos.distanceToSqr(mob.position()) < 1e-4) {
                wallClimber.setClimbing(false);
                stop();
            } else climbPos = mob.position();
        } else if (path != null && !path.isDone()) {
            Vec3 vec3 = Vec3.atBottomCenterOf(path.getNextNodePos()).subtract(mob.position());
            if (vec3.y > 2) {
                wallClimber.setClimbing(true);
                climbPos = mob.position();
                float yRot = (float) -Mth.atan2(vec3.x, vec3.y) * Mth.RAD_TO_DEG;
                mob.setYRot(yRot);
                mob.setYBodyRot(yRot);
            }
        }
    }
}
