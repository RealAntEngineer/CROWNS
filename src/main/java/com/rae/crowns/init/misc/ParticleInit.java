package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleInit {
    public static final DeferredRegister<ParticleType<?>> particles =
            DeferredRegister.create(ForgeRegistries.Keys.PARTICLE_TYPES, CROWNS.MODID);

    public static final RegistryObject<SimpleParticleType> shockwave =
            particles.register("shockwave", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> nukeBlast =
            particles.register("nuke_blast", () -> new SimpleParticleType(true));

    public static void register() {
        particles.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
