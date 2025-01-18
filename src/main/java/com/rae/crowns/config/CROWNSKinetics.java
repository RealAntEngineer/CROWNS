package com.rae.crowns.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CROWNSKinetics extends ConfigBase {

    public CROWNSStress stressValues  = nested(0, CROWNSStress::new, Comments.stress);
    public final ConfigBase.ConfigFloat turbineCoefficient = f(1,0,"turbineCoefficient",Comments.turbineCoefficient);
    public final ConfigBase.ConfigGroup speedValues = group(0,"speedValues",Comments.speed);
    public final ConfigBase.ConfigInt turbineSpeed = i(256,1,256,"turbineSpeed",Comments.turbineCoefficient);

    @Override
    public String getName() {
        return "kinetics";
    }

    private class Comments {
        static String stress = "Fine tune the kinetic stats of individual components";
        static String speed = "Fine tune the speed of generators";
        static String turbineCoefficient = "turbine capacity factor";


    }
}
