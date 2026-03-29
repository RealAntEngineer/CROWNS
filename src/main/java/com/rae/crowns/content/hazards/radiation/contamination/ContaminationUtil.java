package com.rae.crowns.content.hazards.radiation.contamination;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;

public class ContaminationUtil {

    public static void addContamination(LivingEntity entity, double gray) {
        setContamination(entity, getContamination(entity) + gray);
    }

    public static void setContamination(LivingEntity entity, double gray) {
        LazyOptional<IContamination> optional = entity.getCapability(ContaminationProvider.capability);
        if (optional.isPresent()) {
            optional.resolve().get().setRads(gray);
        }
    }

    public static double getContamination(LivingEntity entity) {
        LazyOptional<IContamination> optional = entity.getCapability(ContaminationProvider.capability);
        if (optional.isPresent()) {
            return optional.resolve().get().getRads();
        }
        return 0;
    }
}
