package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SteamCollectorBlock extends WrenchableDirectionalBlock implements IBE<SteamInputBlockEntity> {

    public SteamCollectorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Class<SteamInputBlockEntity> getBlockEntityClass() {
        return SteamInputBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamInputBlockEntity> getBlockEntityType() {
        return BlockEntityInit.STEAM_INPUT.get();
    }
}
