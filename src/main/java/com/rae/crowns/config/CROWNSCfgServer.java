package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import com.simibubi.create.foundation.config.ConfigBase;

public class CROWNSCfgServer extends ConfigBase {

    public final CROWNSKinetics kinetics = nested(0, CROWNSKinetics::new, Comments.kinetics);

    public final CROWNSConstants constants = nested(0,CROWNSConstants::new, Comments.constants);
    @Override
    public String getName() {
        return CROWNS.MODID +".server";
    }

    private static class Comments {
        static String constants ="";
        static String kinetics = "Parameters and abilities of CROWNS's kinetic mechanisms";
    }

}
