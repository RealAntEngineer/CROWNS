package com.rae.formicapi.multiblock;

import com.rae.crowns.CROWNS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public interface IMBController {
    Vec3i getDefaultOffset();
    Vec3i getDefaultSize();
    VoxelShape getGlobalShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context);
    MBStructureBlock getStructure();
    default Vec3i getOffset(Direction facing, boolean mirrorOnDir){
        final int dirMultiply = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE && mirrorOnDir ? -1 : 1;
        Vec3i defaultOffset = getDefaultOffset();
        return switch (facing.getAxis()){
            case Z -> new Vec3i( defaultOffset.getZ(), defaultOffset.getY(), dirMultiply *defaultOffset.getX());
            case Y -> new Vec3i( defaultOffset.getY(), dirMultiply *defaultOffset.getX(),defaultOffset.getZ());
            default -> new Vec3i( dirMultiply *defaultOffset.getX(),defaultOffset.getY(), defaultOffset.getZ());
        };
    }

    default Vec3i getSize(Direction facing){
        Vec3i defaultSize = getDefaultSize();
        return switch (facing.getAxis()){
            case Z -> new Vec3i(defaultSize.getZ(), defaultSize.getY(), defaultSize.getX());
            case Y -> new Vec3i(defaultSize.getY(), defaultSize.getX(),defaultSize.getZ());
            default -> defaultSize;
        };
    }


    default void repairStructure(Level level, BlockPos controlPos, Direction facing) {
        if (level.isClientSide()) return;
        MBStructureBlock structure = getStructure();
        Set<BlockPos> visited = new HashSet<>();
        Queue<Node> toVisit = new ArrayDeque<>();

        Vec3i off = getOffset(facing, false);
        Vec3i size = getSize(facing);
        BlockPos minCorner = controlPos.offset(off);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = controlPos.relative(dir);

            if (isInsideBounds(neighborPos, minCorner, size)) {
                toVisit.add(new Node(dir, neighborPos));
            }
        }
        int i = 0;
        while (!toVisit.isEmpty()) {
            Node node = toVisit.poll();
            BlockState current = level.getBlockState(node.pos);


            if (!current.is(structure)) {
                level.setBlockAndUpdate(node.pos, structure.defaultBlockState().setValue(DirectionalBlock.FACING, node.fromDir.getOpposite()));
                visited.add(node.pos);

                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = node.pos.relative(dir);
                    //System.out.println(neighborPos);

                    if (isInsideBounds(neighborPos, minCorner, size) && !neighborPos.equals(controlPos) && !visited.contains(neighborPos)) {
                        toVisit.add(new Node(dir, neighborPos));
                    }
                }
            }
            i++;
            if (i > 100) {
                CROWNS.LOGGER.warn("More than 100 blocks");
                break;
            }
        }
    }

    default boolean isInsideBounds(BlockPos pos, BlockPos minCorner, Vec3i size) {
        int dx = minCorner.getX() - pos.getX();
        int dy = minCorner.getY() - pos.getY() ;
        int dz = minCorner.getZ() - pos.getZ();
        return dx >= 0 && dx < size.getX() &&
                dy >= 0 && dy < size.getY() &&
                dz >= 0 && dz < size.getZ();
    }
    record Node(Direction fromDir, BlockPos pos){
    }
}
