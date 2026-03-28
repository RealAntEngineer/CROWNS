package com.rae.crowns.content.hazards.radiation.types;

import com.rae.crowns.content.hazards.ItemRadiation;
import com.rae.crowns.content.hazards.radiation.ContaminationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RadiationTypeBetaPlus extends RadiationTypeBase {
    @Override
    public void onUpdate(LivingEntity target, ItemRadiation.DecayContainer container, ItemStack stack) {
        // Aka gamma emitter 2.0
    }
}
