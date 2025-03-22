package com.rae.crowns.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CROWNSConstants extends ConfigBase{
    public final ConfigBase.ConfigFloat realismCoefficient = f(5000000,0,"realismCoef", Comments.bigRocketEngineThrust);
    public final ConfigBase.ConfigFloat assemblyRange = f(3,0,"assemblyRange", Comments.bigRocketEngineThrust);

    @Override
    public String getName() {
        return "constants";
    }
    private static class Comments {
        static String bigRocketEngineThrust ="the thrust in Newtons of the big engine";
    }
}
