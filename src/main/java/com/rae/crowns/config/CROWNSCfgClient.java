package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSCfgClient extends ConfigBase {

    public final ConfigBase.ConfigBool                  thermalVisualisation   = b(false, "thermal_visualisation", CROWNSCfgClient.Comments.thermalVisualisation);
    public final ConfigBase.ConfigFloat                 visualisationThreshold = f(0.1f, 1e-5f, "visualisation_threshold", CROWNSCfgClient.Comments.visualisationThreshold);
    public final ConfigBase.ConfigEnum<FluidVisualMode> fluidStateVisualMode   = e(FluidVisualMode.TPX, "fluid_state_visual_mode");

    @Override
    public @NotNull String getName() {
        return CROWNS.MODID + ".client";
    }

    public enum FluidVisualMode {
        TPX, PH, PS, PHTSX
    }

    private static class Comments {
        static @NotNull String thermalVisualisation   = "See temperature";
        static @NotNull String visualisationThreshold = "Visualisation threshold";

    }

}
