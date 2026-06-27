package com.rae.crowns.content.nuclear.rod;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class GraphiteSleeveBlock extends RotatedPillarBlock implements IBE<GraphiteSleeveBlockEntity> {


    public GraphiteSleeveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<GraphiteSleeveBlockEntity> getBlockEntityClass() {
        return GraphiteSleeveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GraphiteSleeveBlockEntity> getBlockEntityType() {
        return BlockEntityInit.GRAPHITE_SLEEVE.get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {

        if(!level.isClientSide && player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            withBlockEntityDo(level, pos, (be) -> {
                        if (player.isShiftKeyDown()) {
                            be.setOffset(be.offset - 0.1f);
                        } else {
                            be.setOffset(be.offset + 0.1f);
                        }
                    }
            );
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}