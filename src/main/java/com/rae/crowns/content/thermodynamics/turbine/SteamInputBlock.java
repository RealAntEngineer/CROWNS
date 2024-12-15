package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.BlockEntityInit;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SteamInputBlock extends WrenchableDirectionalBlock implements IBE<SteamInputBlockEntity> {

    public SteamInputBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Class<SteamInputBlockEntity> getBlockEntityClass() {
        return SteamInputBlockEntity.class;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, world, pos, newState);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }
    @Override
    public BlockEntityType<? extends SteamInputBlockEntity> getBlockEntityType() {
        return BlockEntityInit.STEAM_INPUT.get();
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        if (level.getBlockEntity(pos) instanceof SteamInputBlockEntity be){
            be.updateSteamFlow = true;
        }
    }
}