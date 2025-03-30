package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SteamCollectorBlock extends WrenchableDirectionalBlock implements IBE<SteamCollectorBlockEntity> {

    public SteamCollectorBlock(Properties pProperties) {
        super(pProperties);
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
