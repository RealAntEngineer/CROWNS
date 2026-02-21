package com.rae.crowns;

import com.rae.crowns.content.ponder.CROWNSPonderPlugin;
import com.rae.crowns.init.client.ParticleTypeInit;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;

public class CROWNSClient {
    public static void clientRegister(@NotNull IEventBus eventBus) {
        PonderIndex.addPlugin(new CROWNSPonderPlugin());
        PartialModelInit.init();
        eventBus.addListener(ParticleTypeInit::registerFactories);
    }
}
