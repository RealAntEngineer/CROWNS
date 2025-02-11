package com.rae.crowns.content.legacy;

import com.rae.crowns.init.BlockEntityInit;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;

import com.simibubi.create.foundation.block.IBE;

import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TurbineBearingBlock extends BearingBlock implements IBE<TurbineBearingBlockEntity> {

    public TurbineBearingBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (worldIn.isClientSide)
                return InteractionResult.SUCCESS;
            withBlockEntityDo(worldIn, pos, be -> {
                if (be.isRunning()) {
                    be.disassemble();
                    return;
                }
                be.assembleNextTick = true;
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


    @Override
    public Class<TurbineBearingBlockEntity> getBlockEntityClass() {
        return TurbineBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineBearingBlockEntity> getBlockEntityType() {
        return BlockEntityInit.TURBINE_BEARING.get();
    }
    public static Couple<Integer> getSpeedRange() {
        return Couple.create(1, 16);
    }

}
