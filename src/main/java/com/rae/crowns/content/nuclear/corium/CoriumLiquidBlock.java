package com.rae.crowns.content.nuclear.corium;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;


public class CoriumLiquidBlock extends LiquidBlock {

    public CoriumLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0).setValue(CoriumFluid.POWER, 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CoriumFluid.POWER);
    }

    protected @NotNull FluidState getFluidState(BlockState state) {
        int p = state.getValue(CoriumFluid.POWER);
        return super.getFluidState(state).setValue(CoriumFluid.POWER, p);
    }
}