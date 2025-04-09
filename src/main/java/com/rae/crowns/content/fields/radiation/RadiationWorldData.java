package com.rae.crowns.content.fields.radiation;

import net.minecraft.core.SectionPos;

import java.util.HashMap;
import java.util.Map;

public class RadiationWorldData {
    private final Map<SectionPos, RadiationData> sectionMap = new HashMap<>();

    public RadiationData getOrCreate(SectionPos section) {
        return sectionMap.computeIfAbsent(section, k -> new RadiationData());
    }

    public RadiationData getIfExists(SectionPos section) {
        return sectionMap.get(section);
    }
}