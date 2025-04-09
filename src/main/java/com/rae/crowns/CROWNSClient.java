package com.rae.crowns;

import com.rae.crowns.content.ponder.CROWNSPonderPlugin;
import com.rae.crowns.init.client.ParticleTypeInit;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = CROWNS.MODID, dist = Dist.CLIENT)
public class CROWNSClient {
    public CROWNSClient(IEventBus eventBus) {
        PonderIndex.addPlugin(new CROWNSPonderPlugin());

        eventBus.addListener(ParticleTypeInit::registerFactories);
    }
}
