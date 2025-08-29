package com.rae.crowns.content.nuclear;

import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class ControlledAssemblyBlock extends AssemblyBlock implements IRotate {

    public ControlledAssemblyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(AssemblyBlock.AXIS).equals(face.getAxis());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return null;
    }
}
