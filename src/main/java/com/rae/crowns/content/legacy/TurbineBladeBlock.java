package com.rae.crowns.content.legacy;

import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineBladeBlock extends DirectedDirectionalBlock{// implements IBE<FanBladeBlockEntity> {
    public TurbineBladeBlock(Properties pProperties) {
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

    /*@Override
    public Class<FanBladeBlockEntity> getBlockEntityClass() {
        return null;
    }

    @Override
    public BlockEntityType<? extends FanBladeBlockEntity> getBlockEntityType() {
        return null;
    }*/
}
