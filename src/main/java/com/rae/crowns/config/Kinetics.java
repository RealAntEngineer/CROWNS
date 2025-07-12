package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;

public class Kinetics extends ConfigBase {

    //public CROWNSStress stressValues  = nested(0, CROWNSStress::new, Comments.stress);
    public final ConfigBase.ConfigGroup turbineValues = group(0,"turbineValues",Comments.turbineStage);

    public final ConfigBase.ConfigFloat turbineCoefficient = f(1,0,"turbineCoefficient",Comments.turbineCoefficient);
    public final ConfigBase.ConfigInt turbineSpeed = i(256,1,256,"turbineSpeed",Comments.turbineCoefficient);

    @Override
    public String getName() {
        return "kinetics";
    }

    private static class Comments {
        static String turbineStage = "Fine tune the speed and capacity of turbine stages";
        static String turbineCoefficient = "turbine capacity factor";
    }
}
