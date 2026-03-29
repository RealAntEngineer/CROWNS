package com.rae.crowns.init.misc;

import com.rae.crowns.content.hazards.radiation.effects.EffectBoneMarrowSuppression;
import com.rae.crowns.content.hazards.radiation.effects.EffectDamagedSkin;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EffectsInit {
    public static final DeferredRegister<MobEffect> effects = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "crowns");

    public static final RegistryObject<MobEffect>
        BONE_MARROW_SUPPRESSION = effects.register("bone_marrow_suppression", EffectBoneMarrowSuppression::new),
        RADIODERMATITIS = effects.register("radiodermatitis", EffectDamagedSkin::new);

    public static void register() {
        effects.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
