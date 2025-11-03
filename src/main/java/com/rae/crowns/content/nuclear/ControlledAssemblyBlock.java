package com.rae.crowns.content.nuclear;

import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ControlledAssemblyBlock extends AssemblyBlock implements IRotate {

    public ControlledAssemblyBlock(@NotNull Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, @NotNull BlockState state, @NotNull Direction face) {
        return state.getValue(AssemblyBlock.AXIS).equals(face.getAxis());
    }

    @Override
    public Direction.@Nullable Axis getRotationAxis(BlockState state) {
        return null;
    }
}
