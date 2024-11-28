package com.rae.crowns.content.thermals.turbine;

import com.rae.crowns.init.BlockEntityInit;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CompressorStageBlock extends DirectionalKineticBlock implements IBE<TurbineStageBlockEntity> {
    public CompressorStageBlock(Properties pProperties) {
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
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
    @Override
    public boolean showCapacityWithAnnotation() {
        return true;
    }

    @Override
    public Class<TurbineStageBlockEntity> getBlockEntityClass() {
        return TurbineStageBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineStageBlockEntity> getBlockEntityType() {
        return BlockEntityInit.TURBINE_STAGE.get();
    }
}