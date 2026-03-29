package com.rae.crowns.content.hazards.radiation.mobeffects;

import com.rae.crowns.init.misc.EffectsInit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BoneMarrowSuppression { // Diabolical class name
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = entity.getEffect(EffectsInit.BONE_MARROW_SUPPRESSION.get());

        if (effect == null) return;

        if (entity.getHealth() <= 6 && effect.getAmplifier() == 0) return; // <3 guard statements
        event.setCanceled(true);
    }
}
