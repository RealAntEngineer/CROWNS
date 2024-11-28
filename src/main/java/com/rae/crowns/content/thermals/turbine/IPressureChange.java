package com.rae.crowns.content.thermals.turbine;

import net.minecraft.core.BlockPos;

public interface IPressureChange {

    float pressureRatio();
    BlockPos getBlockPos();
}
