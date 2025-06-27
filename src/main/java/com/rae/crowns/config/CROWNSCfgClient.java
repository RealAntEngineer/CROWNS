package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSCfgClient extends ConfigBase {


    public final ConfigGroup nuclear = new ConfigGroup("nuclear",0, Comments.nuclear);
    public final ConfigBool nuclearParticle = b(true, "nuclear_particle",Comments.nuclearParticle);
    @Override
    public @NotNull String getName() {
        return CROWNS.MODID +".client";
    }

    private static class Comments {
        static String units = "Units used";
        static String nuclear = "Graphic config for nuclear";
        static String nuclearParticle = "Radiation Particles";

    }

}
