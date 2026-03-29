package com.rae.crowns.content.hazards.radiation.types;

import com.rae.crowns.content.hazards.ItemRadiation;
import com.rae.crowns.init.misc.EffectsInit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RadiationTypeBetaMinus extends RadiationTypeBase {
    @Override
    public void onUpdate(LivingEntity target, ItemRadiation.DecayContainer container, ItemStack stack) {
        // Electrons burn your skin and do the same thing as alphas but weaker, so a it's a little spicier
        MobEffectInstance effect = new MobEffectInstance(EffectsInit.RADIODERMATITIS.get(), 100);

        double flux = (container.specific_activity * container.beta_minus) * stack.getCount();
        double P = flux * (container.beta_energy * 1.602177e-13);
        double dose = (P * 0.6) / 0.1; // tweak this for absorption

        if (flux > 1.8e8) { // I pulled this number out of my ass
            target.addEffect(effect);
            if (dose > 1) target.hurt(target.damageSources().generic(), 0.05F);
        }
    }
}
