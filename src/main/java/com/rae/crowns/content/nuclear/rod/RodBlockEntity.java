package com.rae.crowns.content.nuclear.rod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RodBlockEntity extends BaseRodContainer {


    public RodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setNeutronBaseProperties(0.0f, 0.0f, 0.0f);
    }
}