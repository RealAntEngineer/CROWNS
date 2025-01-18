package com.rae.crowns.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CROWNSNuclear extends ConfigBase{
    public final ConfigBase.ConfigFloat realismCoefficient = f(5000000,0,"realismCoef", Comments.realismCoef);
    public final ConfigBase.ConfigFloat assemblyRange = f(3,0,"assemblyRange", Comments.assemblyRange);

    @Override
    public String getName() {
        return "nuclear";
    }
    private static class Comments {
        static String realismCoef ="make reactor be faster";
        static String assemblyRange ="the maximum distance for radiation influence on fission, the bigger the range," +
                "the better big reactor will perform. Huge performance impact don't make it higher than 10";

    }
}
