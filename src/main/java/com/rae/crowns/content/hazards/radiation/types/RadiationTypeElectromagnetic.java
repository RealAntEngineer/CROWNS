package com.rae.crowns.content.hazards.radiation.types;

import com.rae.crowns.content.hazards.ItemRadiation;
import com.rae.crowns.content.hazards.radiation.ContaminationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RadiationTypeElectromagnetic extends RadiationTypeBase {

    @Override
    public void onUpdate(LivingEntity target, ItemRadiation.DecayContainer container, ItemStack stack) {
        // Super basic
        ContaminationUtil.addContamination(target, (container.multiply(stack.getCount()).getRoentgen(0.01D) * 0.0096) / 20); // Constant for R to Gy
    }
}
