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
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SmarterClimberNodeEvaluator extends WalkNodeEvaluator {
    private final Long2ObjectArrayMap<Climbable> climbableCache = new Long2ObjectArrayMap<>();
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
    public int getNeighbors(Node[] nodes, Node start) {
        int size;

        Direction direction = getClimbableDirection(start.x, start.y, start.z);
        if (direction != null) {
            size = 0;
            BlockPos pos = start.asBlockPos();
            Node node = getClimbable(pos.offset(0, 1, 0), direction);
            if (node != null) nodes[size++] = node;
            else {
                node = findAcceptedNode(start.x, start.y + 1, start.z, 0, start.y + 1, Direction.UP, start.type);
                if (node != null) nodes[size++] = node;
            }
            node = getClimbable(pos.offset(0, -1, 0), direction);
            if (node != null) nodes[size++] = node;
            else {
                node = findAcceptedNode(start.x, start.y - 1, start.z, 0, pos.getY(), Direction.DOWN, start.type);
                if (node != null) nodes[size++] = node;
            }
            node = getClimbable(pos.relative(direction.getClockWise()), direction);
            if (node != null) nodes[size++] = node;
            node = getClimbable(pos.relative(direction.getCounterClockWise()), direction);
            if (node != null) nodes[size++] = node;
        }

        else {
            size = super.getNeighbors(nodes, start);
            Node node = getClimbable(start.x, start.y + 1, start.z - 1, Direction.NORTH);
            if (node != null) nodes[size++] = node;
            node = getClimbable(start.x + 1, start.y + 1, start.z, Direction.EAST);
            if (node != null) nodes[size++] = node;
            node = getClimbable(start.x, start.y + 1, start.z + 1, Direction.SOUTH);
            if (node != null) nodes[size++] = node;
            node = getClimbable(start.x - 1, start.y + 1, start.z, Direction.WEST);
            if (node != null) nodes[size++] = node;
        }

        return size;
    }

    protected @Nullable Node getClimbable(int x, int y, int z, Direction direction) {
        Climbable climbable = climbableCache.computeIfAbsent(Node.createHash(x, y, z), l -> findClimbable(x, y, z, direction));
        return climbable != null ? climbable.node : null;
    }

    protected @Nullable Node getClimbable(BlockPos pos, Direction direction) {
        return getClimbable(pos.getX(), pos.getY(), pos.getZ(), direction);
    }

    protected @Nullable Direction getClimbableDirection(int x, int y, int z) {
        Climbable climbable = climbableCache.get(Node.createHash(x, y, z));
        return climbable != null ? climbable.direction : null;
    }

    public boolean isClimbing(Path path) {
        Node node = path.getNextNode();
        if (getClimbableDirection(node.x, node.y, node.z) != null) return true;
        node = path.getPreviousNode();
        return node != null && getClimbableDirection(node.x, node.y, node.z) != null;
    }

    protected @Nullable Climbable findClimbable(int x, int y, int z, Direction direction) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).isFaceSturdy(level, pos, direction.getOpposite())) return null;
        BlockPathTypes blockType = getCachedBlockType(mob, x, y, z);
        float malus;
        if (blockType == BlockPathTypes.BLOCKED) {
            blockType = BlockPathTypes.WALKABLE;
            malus = 0;
        } else {
            malus = mob.getPathfindingMalus(blockType);
            if (malus < 0) return null;
        }

        float width = (maxHitbox != null ? maxHitbox.width : mob.getBbWidth()) * 0.5f;
        AABB aabb = new AABB(x + 0.5 - width, y, z + 0.5 - width,
                x + 0.5 + width, y + (maxHitbox != null ? maxHitbox.height : mob.getBbHeight()),
                z + 0.5 + width).move(new Vec3(direction.step()).scale(-width - 0.51f));
        for (VoxelShape shape: (Iterable<VoxelShape>) () -> new BlockCollisions<>(level, mob, aabb, false,
                (pos1, shape1) -> level.getBlockState(pos1).is(BlockTags.LEAVES) ? Shapes.empty() : shape1))
            if (!shape.isEmpty()) return null;

        Climbable climbable = new Climbable(getNode(x, y, z), direction);
        climbable.node.type = blockType;
        climbable.node.costMalus = malus;
        return climbable;
    }

    protected record Climbable(Node node, Direction direction) {}
}
