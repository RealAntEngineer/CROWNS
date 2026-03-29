package com.rae.crowns.content.hazards.radiation.types;

import com.rae.crowns.content.hazards.ItemRadiation;
import com.rae.crowns.content.hazards.radiation.ContaminationUtil;
import com.rae.crowns.init.misc.EffectsInit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RadiationTypeAlpha extends RadiationTypeBase {
    @Override
    public void onUpdate(LivingEntity target, ItemRadiation.DecayContainer container, ItemStack stack) {
        MobEffectInstance effect = target.getEffect(EffectsInit.RADIODERMATITIS.get());

        if (effect != null || target.getHealth() <= 6) {
            final double dose = (container.specific_activity * stack.getCount()) * (container.alpha_energy * 1.602177e-13) / 0.1;
            final double health = target.getHealth() / target.getMaxHealth();
            final double factor = health / 4.0 * 3.0 + 0.25d;

            ContaminationUtil.addContamination(target, (dose / factor) / 20);
        }
    }
}
