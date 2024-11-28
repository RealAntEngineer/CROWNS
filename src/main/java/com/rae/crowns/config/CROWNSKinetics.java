package com.rae.crowns.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CROWNSKinetics extends ConfigBase {
    public CROWNSStress stressValues  = nested(1, CROWNSStress::new, Comments.stress);

    @Override
    public String getName() {
        return "kinetics";
    }

    private class Comments {
        static String stress = "Fine tune the kinetic stats of individual components";

    }
}
