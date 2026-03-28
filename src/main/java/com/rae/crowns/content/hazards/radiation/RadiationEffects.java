package com.rae.crowns.content.hazards.radiation;

import com.rae.crowns.init.misc.EffectsInit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RadiationEffects {
    // TODO: add vomiting

    public static void hematopoieticSubsyndrome(LivingEntity target) {
        MobEffectInstance confusion = new MobEffectInstance(MobEffects.CONFUSION, 100, 1);
        MobEffectInstance suppression = new MobEffectInstance(EffectsInit.BONE_MARROW_SUPPRESSION.get(), 100);
        MobEffectInstance weakness = new MobEffectInstance(MobEffects.WEAKNESS, 100);

        if (Math.random() < 0.01) {
            target.addEffect(confusion);
        }

        if (Math.random() < 0.01) {
            target.addEffect(weakness);
        }

        target.addEffect(suppression);
    }

    public static void gastrointestinalSubsyndrome(LivingEntity target) {
        MobEffectInstance confusion = new MobEffectInstance(MobEffects.CONFUSION, 200, 2);
        MobEffectInstance suppression = new MobEffectInstance(EffectsInit.BONE_MARROW_SUPPRESSION.get(), 100, 1);
        MobEffectInstance weakness = new MobEffectInstance(MobEffects.WEAKNESS, 100, 2);
        MobEffectInstance fatigue = new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100);
        MobEffectInstance death = new MobEffectInstance(MobEffects.POISON, 40);
        MobEffectInstance darkness = new MobEffectInstance(MobEffects.DARKNESS, 100, 2);

        if (Math.random() < 0.01) {
            target.addEffect(darkness);
        }

        if (Math.random() < 0.01) {
            target.addEffect(confusion);
        }

        if (Math.random() < 0.05) {
            target.addEffect(weakness);
        }

        if (Math.random() < 0.008) {
            target.addEffect(death); // Lebron james hairline
        }

        target.addEffect(fatigue);
        target.addEffect(suppression);
    }

    public static void neurovascularSubsyndrome(LivingEntity target) {
        MobEffectInstance suppression = new MobEffectInstance(EffectsInit.BONE_MARROW_SUPPRESSION.get(), 100, 2);
        MobEffectInstance death = new MobEffectInstance(MobEffects.HARM, 20);

        target.addEffect(death);
        target.addEffect(suppression);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;

        if (player.isCreative()) return;

        double contamination = ContaminationUtil.getContamination(player);

        if (contamination >= 10) {
            // Neurovascular subsyndrome is lethal
            neurovascularSubsyndrome(player);
        } else if (contamination >= 4) {
            // Gastrointestinal subsyndrome is really bad
            gastrointestinalSubsyndrome(player);
        } else if (contamination >= 1) {
            // Hematopoietic subsyndrome to be implemented: bone marrow suppression, nausea and vomiting occurs here
            hematopoieticSubsyndrome(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ContaminationUtil.setContamination(event.getEntity(), 0);
    }
}
