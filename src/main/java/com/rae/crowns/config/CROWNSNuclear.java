package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSNuclear extends ConfigBase {

    public final ConfigBase.ConfigBool explosion = b(true, "explosion", Comments.explosion);
    public final ConfigBase.ConfigFloat radiationRange = f(4, 0, "radiationRange", Comments.radiationRange);
    public final ConfigBase.ConfigFloat neutronFluxMultiplicator = f(0.8f, 0, "neutronFluxMultiplicator", Comments.neutronFluxMultiplicator);
    public final ConfigBase.ConfigFloat negativeThermalCoef = f(0.0075f, 0, "negativeThermalCoef", Comments.negativeThermalCoef);

    @Override
    public @NotNull String getName() {
        return "nuclear_v2";
    }

    private static class Comments {
        static @NotNull String explosion = "activate explosion";
        static @NotNull String radiationRange = "the maximum distance for radiation influence on fission, the bigger the range," +
                "the better big reactor will perform. Huge performance impact don't make it higher than 10";
        static @NotNull String neutronFluxMultiplicator = " decrease it to make reactor less reactive, control how much neutron each fission gives out (neutronFluxMultiplicator * 2.5)";
        static @NotNull String negativeThermalCoef = "increase it to decrease the temperature, make neutron less likely to impact" +
                " when temperature is higher ((temperature - 200) * negativeThermalCoef)";

    }
}
