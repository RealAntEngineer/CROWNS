package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class CROWNSCfgClient extends ConfigBase {

    public final ConfigBase.ConfigBool                  thermalVisualisation   = b(false, "thermal_visualisation", Comments.thermalVisualisation);
    public final ConfigBase.ConfigFloat                 visualisationThreshold = f(10f, 1e-5f, "visualisation_threshold", Comments.visualisationThreshold);
    public final ConfigBase.ConfigEnum<FluidVisualMode> fluidStateVisualMode   = e(FluidVisualMode.TPX, "fluid_state_visual_mode",Comments.fluidStateVisualMode);

    @Override
    public String getName() {
        return CROWNS.MODID + ".client";
    }

    public enum FluidVisualMode {
        TPX, PH, PS, PHTSX
    }

    private static class Comments {
        static String thermalVisualisation   = "See temperature";
        static String visualisationThreshold = "Visualisation threshold";
        static String fluidStateVisualMode   = "How the fluid state is rendered";
    }
}