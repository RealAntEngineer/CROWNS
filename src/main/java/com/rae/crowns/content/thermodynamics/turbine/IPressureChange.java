package com.rae.crowns.content.thermodynamics.turbine;

import net.minecraft.core.BlockPos;

public interface IPressureChange {

    float pressureRatio();
    BlockPos getBlockPos();
}
