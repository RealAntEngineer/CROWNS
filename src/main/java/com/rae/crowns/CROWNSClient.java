package com.rae.crowns;

import com.rae.crowns.init.ParticleTypeInit;
import net.minecraftforge.eventbus.api.IEventBus;

public class CROWNSClient {
    public static void clientRegister(IEventBus eventBus) {

        eventBus.addListener(ParticleTypeInit::registerFactories);
    }
}
