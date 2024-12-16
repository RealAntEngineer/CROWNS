package com.rae.crowns;

import com.rae.crowns.init.ParticleTypeInit;
import com.rae.crowns.init.PonderInit;
import net.minecraftforge.eventbus.api.IEventBus;

public class CROWNSClient {
    public static void clientRegister(IEventBus eventBus) {
        PonderInit.register();
        eventBus.addListener(ParticleTypeInit::registerFactories);
    }
}
