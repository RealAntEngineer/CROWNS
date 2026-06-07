package com.rae.crowns.content.nuclear.detectors;

import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class NeutronDetector extends WrenchableDirectionalBlock {

    public NeutronDetector(@NotNull Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH));

    }
}
