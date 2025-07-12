package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;

public class Nuclear extends ConfigBase {
    public final ConfigBase.ConfigFloat realismCoefficient = f(5000000,0,"realismCoef", Comments.realismCoef);
    public final ConfigBase.ConfigFloat radiationRange = f(4,0,"radiationRange", Comments.radiationRange);

    @Override
    public String getName() {
        return "nuclear";
    }
    private static class Comments {
        static String realismCoef ="make reactor be faster";
        static String radiationRange ="the maximum distance for radiation influence on fission, the bigger the range," +
                "the better big reactor will perform. Huge performance impact don't make it higher than 10";

    }
}
