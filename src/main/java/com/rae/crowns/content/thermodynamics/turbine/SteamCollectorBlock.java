package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SteamCollectorBlock extends WrenchableDirectionalBlock implements IBE<SteamCollectorBlockEntity> {

    public SteamCollectorBlock(Properties pProperties) {
        super(pProperties);
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
    public Class<SteamCollectorBlockEntity> getBlockEntityClass() {
        return SteamCollectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamCollectorBlockEntity> getBlockEntityType() {
        return BlockEntityInit.STEAM_COLLECTOR.get();
    }
}
