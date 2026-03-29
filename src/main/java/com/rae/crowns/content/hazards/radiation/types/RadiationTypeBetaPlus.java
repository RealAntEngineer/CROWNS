package com.rae.crowns.content.hazards.radiation.types;

import com.rae.crowns.content.hazards.radiation.ItemRadiation;
import com.rae.crowns.content.hazards.radiation.contamination.ContaminationUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RadiationTypeBetaPlus extends RadiationTypeBase {
    @Override
    public void onUpdate(LivingEntity target, ItemRadiation.DecayContainer container, ItemStack stack) {
        // Aka gamma emitter 2.0
        ContaminationUtil.addContamination(target, getReontgen(0.01D, container.specific_activity * container.beta_plus));
    }

    public static double getReontgen(double distance, double gammas) { // in R/s
        final double airMassAbsorptionCoefficient = 0.029D;

        return getEnergyFluence(distance, gammas) * airMassAbsorptionCoefficient * (1.828e-11);
    }

    public static double getEnergyFluence(double distance, double gammas) {
        return (gammas * 511 / 1000) / (4 * Math.PI * Math.pow(distance, 2));
    }
}
