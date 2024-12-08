package com.rae.crowns.content.thermodynamics;

import com.google.common.collect.ImmutableMap;
import com.rae.crowns.init.BlockEntityInit;
import com.rae.crowns.init.BlockInit;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class HeatExchangerBlock extends DirectionalBlock implements ProperWaterloggedBlock, IBE<HeatExchangerBlockEntity> {
    public HeatExchangerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(IN, true)
                .setValue(OUT, true));
    }
    public static final BooleanProperty IN = BooleanProperty.create("in");
    public static final BooleanProperty OUT = BooleanProperty.create("out");

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING,WATERLOGGED,IN,OUT));

    }
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = withWater(this.defaultBlockState().setValue(FACING, context.getClickedFace()), context);
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace(), -1));
        BlockState oppositeState = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace(), 1));

        if (clickedState.is(BlockInit.HEAT_EXCHANGER.get())  && clickedState.getValue(FACING).getAxis() == context.getClickedFace().getAxis()){
            state.setValue(FACING, clickedState.getValue(FACING));
            if (clickedState.getValue(FACING).getAxisDirection() ==  context.getClickedFace().getAxisDirection()){
                state.setValue(OUT, false);
            }
            else {
                state.setValue(IN, false);

            }
        }
        else if (oppositeState.is(BlockInit.HEAT_EXCHANGER.get()) && oppositeState.getValue(FACING).getAxis() == context.getClickedFace().getAxis()){
            state.setValue(FACING, oppositeState.getValue(FACING));
            if (oppositeState.getValue(FACING).getAxisDirection() ==  context.getClickedFace().getAxisDirection()){
                state.setValue(OUT, false);
            }
            else {
                state.setValue(IN, false);

            }
        }

        return state;
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState pState) {
        return fluidState(pState);
    }
    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState pState, @NotNull Direction pDirection, @NotNull BlockState pNeighborState,
                                           @NotNull LevelAccessor pLevel, @NotNull BlockPos pCurrentPos, @NotNull BlockPos pNeighborPos) {
        updateWater(pLevel, pState, pCurrentPos);
        if (pNeighborState.is(this.asBlock())) {
            if (pState.getValue(FACING) != pNeighborState.getValue(FACING)) {
                pState.setValue(FACING, pNeighborState.getValue(FACING));
            }
        }
        return pState;
    }

    public @NotNull VoxelShape getShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return AllShapes.EIGHT_VOXEL_POLE.get(pState.getValue(FACING).getAxis());
    }

    @Override
    public Class<HeatExchangerBlockEntity> getBlockEntityClass() {
        return HeatExchangerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeatExchangerBlockEntity> getBlockEntityType() {
        return BlockEntityInit.HEAT_EXCHANGER.get();
    }
}
