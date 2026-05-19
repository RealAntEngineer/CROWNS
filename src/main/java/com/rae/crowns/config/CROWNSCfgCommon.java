package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSCfgCommon extends ConfigBase {
    //yes it needs to be on the common side...
    public final ConfigBase.ConfigGroup nuclear         = new ConfigBase.ConfigGroup("nuclear", 0, Comments.nuclear);
    public final ConfigBase.ConfigBool  nuclearParticle = b(true, "nuclear_particle", Comments.nuclearParticle);

    public @NotNull String getName() {
        return CROWNS.MODID + ".common";
    }

    private static class Comments {
        static String nuclear         = "Graphic config for nuclear";
        static String nuclearParticle = "Radiation Particles";

    }
}
