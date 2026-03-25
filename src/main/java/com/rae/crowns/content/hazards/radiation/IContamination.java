package com.rae.crowns.content.hazards.radiation;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface IContamination {
    double getRads();
    void setRads(double value);
}
