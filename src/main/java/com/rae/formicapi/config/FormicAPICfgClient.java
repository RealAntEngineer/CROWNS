package com.rae.formicapi.config;


import com.rae.formicapi.FormicAPI;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class FormicAPICfgClient extends ConfigBase {


    public final CROWNSUnits units = nested(0, CROWNSUnits::new, Comments.units);
    @Override
    public @NotNull String getName() {
        return FormicAPI.MODID +".client";
    }

    private static class Comments {
        static String units = "Units used";
        static String nuclear = "Graphic config for nuclear";
        static String nuclearParticle = "Radiation Particles";

    }

}
