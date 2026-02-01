package com.rae.crowns.content.thermodynamics.compressor;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class CompressorBlock extends DirectionalKineticBlock implements IBE<CompressorBlockEntity>, ICogWheel {
    public CompressorBlock(@NotNull Properties pProperties) {
        super(pProperties);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return true;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;//face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.@NotNull Axis getRotationAxis(@NotNull BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean showCapacityWithAnnotation() {
        return true;
    }

    @Override
    public @NotNull Class<CompressorBlockEntity> getBlockEntityClass() {
        return CompressorBlockEntity.class;
    }

    @Override
    public @NotNull BlockEntityType<? extends CompressorBlockEntity> getBlockEntityType() {
        return BlockEntityInit.COMPRESSOR.get();
    }
}