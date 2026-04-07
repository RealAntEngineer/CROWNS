package com.rae.crowns.content.rendering.particle;

import com.rae.crowns.init.misc.ParticleInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

// I hate particles

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleRenderer {

    @SubscribeEvent
    public static void registerParticles(@NotNull RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ParticleInit.shockwave.get(),
                ShockwaveParticle.Provider::new
        );

        event.registerSpriteSet(
                ParticleInit.nukeBlast.get(),
                SmokeParticle.Provider::new
        );
    }
}
