package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSCfgServer extends ConfigBase {

    public final CROWNSKinetics kinetics   = nested(0, CROWNSKinetics::new, Comments.kinetics);
    public final CROWNSNuclear nuclear = nested(0, CROWNSNuclear::new, Comments.nuclear);
    public final CROWNSThermal thermal = nested(0, CROWNSThermal::new, Comments.thermal);

    @Override
    public @NotNull String getName() {
        return CROWNS.MODID + ".server";
    }

    private static class Comments {
        static @NotNull String nuclear  = "Parameter and constants for nuclear reactors";
        static @NotNull String kinetics = "Parameters and abilities of CROWNS's kinetic mechanisms";
        static @NotNull String thermal  = "How heat is transferred. Changes can create instability and world corruption, HERE BE DRAGONS ";
    }

}
