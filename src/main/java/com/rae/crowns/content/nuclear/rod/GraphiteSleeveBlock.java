package com.rae.crowns.content.nuclear.rod;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
}