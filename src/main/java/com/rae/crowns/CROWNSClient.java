package com.rae.crowns;

import com.rae.crowns.content.ponder.CROWNSPonderPlugin;
import com.rae.crowns.init.client.PartialModelInit;
import com.rae.crowns.init.client.ParticleTypeInit;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraftforge.eventbus.api.IEventBus;
//@OnlyIn(Dist.CLIENT)
public class CROWNSClient {
    public static void clientRegister(IEventBus eventBus) {
        CROWNS.LOGGER.info("CROWNS Client registration started");
        PonderIndex.addPlugin(new CROWNSPonderPlugin());
        PartialModelInit.init();

        eventBus.addListener(ParticleTypeInit::registerFactories);
    }
}
