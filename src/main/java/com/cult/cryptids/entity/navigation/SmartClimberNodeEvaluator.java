package com.cult.cryptids.entity.navigation;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SmartClimberNodeEvaluator extends WalkNodeEvaluator {
    private final Long2ObjectArrayMap<Node> climbableCache = new Long2ObjectArrayMap<>();
    private @Nullable EntityDimensions minHitbox, maxHitbox;

    public void shiftsHitbox(@Nullable EntityDimensions minHitbox, @Nullable EntityDimensions maxHitbox) {
        this.minHitbox = minHitbox;
        this.maxHitbox = maxHitbox;
    }

    @Override
    public void prepare(PathNavigationRegion region, Mob mob) {
        super.prepare(region, mob);
        if (minHitbox != null) {
            entityWidth = entityDepth = Mth.floor(minHitbox.width + 1);
            entityHeight = Mth.floor(minHitbox.height + 1);
        }
    }

    @Override
    public void done() {
        climbableCache.clear();
        super.done();
    }

    @Override
    protected @Nullable Node findAcceptedNode(int x, int y, int z, int jump, double floor, Direction direction, BlockPathTypes standingOn) {
        Node node = super.findAcceptedNode(x, y, z, jump, floor, direction, standingOn);
        return node != null ? node : getClimbable(x, y, z, floor, direction);
    }

    protected @Nullable Node getClimbable(int x, int y, int z, double floor, Direction direction) {
        return climbableCache.computeIfAbsent(((((((x & 0x1ffffffL) << 25) + (z & 0x1ffffffL)) << 12) +
                        (y & 0xfffL)) << 2) + direction.get2DDataValue(), l -> findPeak(x, y, z, floor, direction));
    }

    private @Nullable Node findPeak(int x, int y, int z, double floor, Direction direction) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (level.getBlockState(pos).isFaceSturdy(level, pos, direction.getOpposite())) pos.move(0, 1, 0);
        if (pos.getY() == y) return null;
        BlockPathTypes blockType = getBlockPathType(mob, pos);
        float malus = mob.getPathfindingMalus(blockType);
        if (malus < 0) return null;

        float width = (maxHitbox != null ? maxHitbox.width : mob.getBbWidth()) * 0.5f;
        AABB aabb = new AABB(x + 0.5 - width, floor, z + 0.5 - width, x + 0.5 + width,
                getFloorLevel(pos) + (maxHitbox != null ? maxHitbox.height : mob.getBbHeight()),
                z + 0.5 + width).move(new Vec3(direction.step()).scale(-width - 0.51f));
        for (VoxelShape shape: (Iterable<VoxelShape>) () -> new BlockCollisions<>(level, mob, aabb, false,
                (pos1, shape1) -> level.getBlockState(pos1).is(BlockTags.LEAVES) ? Shapes.empty() : shape1))
            if (!shape.isEmpty()) return null;


        Node node = getNode(pos.getX(), pos.getY(), pos.getZ());
        node.type = blockType;
        node.costMalus = malus;
        return node;
    }
}
