package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSCfgServer extends ConfigBase {

    public final CROWNSKinetics kinetics = nested(0, CROWNSKinetics::new, Comments.kinetics);

    public final CROWNSNuclear nuclear = nested(0, CROWNSNuclear::new, Comments.nuclear);
    public final CROWNSConduction conduction = nested(0, CROWNSConduction::new, Comments.conduction);
    @Override
    public @NotNull String getName() {
        return CROWNS.MODID +".server";
    }

    private static class Comments {
        static String nuclear ="Parameter and constants for nuclear reactors";
        static String kinetics = "Parameters and abilities of CROWNS's kinetic mechanisms";
        static String conduction = "How heat is transferred. Changes can create instability and world corruption, HERE BE DRAGONS ";
    }

}
