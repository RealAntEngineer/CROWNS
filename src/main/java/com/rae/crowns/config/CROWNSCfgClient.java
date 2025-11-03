package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSCfgClient extends ConfigBase {

    public final ConfigBase.ConfigBool thermalVisualisation = b(true, "thermal_visualisation", CROWNSCfgClient.Comments.thermalVisualisation);
    public final ConfigBase.ConfigFloat visualisationThreshold = f(0.1f, 1e-5f, "visualisation_threshold", CROWNSCfgClient.Comments.visualisationThreshold);

    @Override
    public @NotNull String getName() {
        return CROWNS.MODID + ".client";
    }

    private static class Comments {
        static @NotNull String thermalVisualisation = "See temperature";
        static @NotNull String visualisationThreshold = "Visualisation threshold";

    }

}
