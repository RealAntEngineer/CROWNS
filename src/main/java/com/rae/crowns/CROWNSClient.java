package com.rae.crowns;

import com.rae.crowns.content.ponder.CROWNSPonderPlugin;
import com.rae.crowns.init.client.ParticleTypeInit;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraftforge.eventbus.api.IEventBus;

public class CROWNSClient {
    public static void clientRegister(IEventBus eventBus) {
        PonderIndex.addPlugin(new CROWNSPonderPlugin());

        eventBus.addListener(ParticleTypeInit::registerFactories);
    }
}
