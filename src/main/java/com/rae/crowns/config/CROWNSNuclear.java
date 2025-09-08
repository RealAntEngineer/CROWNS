package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSNuclear extends ConfigBase {
    public final ConfigBase.ConfigFloat realismCoefficient = f(5e9f,0,"realismCoef", Comments.realismCoef);
    public final ConfigBase.ConfigFloat radiationRange = f(4,0,"radiationRange", Comments.radiationRange);
    public final ConfigBase.ConfigFloat easeCoef = f(0.8f,0,"easeCoef", Comments.easeCoef);
    public final ConfigBase.ConfigFloat negativeThermalCoef = f(0.0075f,0,"negativeThermalCoef", Comments.negativeThermalCoef);

    @Override
    public @NotNull String getName() {
        return "nuclear";
    }
    private static class Comments {
        static String realismCoef = "make reactor reach equilibrium faster, don't change it will a reactor is running";
        static String radiationRange = "the maximum distance for radiation influence on fission, the bigger the range," +
                "the better big reactor will perform. Huge performance impact don't make it higher than 10";
        static String easeCoef = " decrease it to make reactor less reactive, control how much neutron each fission gives out (easeCoef * 2.5)";
        static String negativeThermalCoef ="increase it to decrease the temperature, make neutron less likely to impact" +
                " when temperature is higher ((temperature - 200) * negativeThermalCoef)";

    }
}