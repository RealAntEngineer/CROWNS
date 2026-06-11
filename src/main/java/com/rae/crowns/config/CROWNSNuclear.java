package com.rae.crowns.config;


import com.rae.crowns.Constants;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CROWNSNuclear extends ConfigBase {

    public final ConfigBase.ConfigBool explosion = b(true, "explosion", Comments.explosion);
    public final ConfigBase.ConfigFloat radiationRange = f(4, 0, "radiationRange", Comments.radiationRange);
    public final ConfigBase.ConfigFloat neutronFluxMultiplicator = f(1f, 0, "neutronFluxMultiplicator", Comments.neutronFluxMultiplicator);
    public final ConfigBase.ConfigFloat negativeThermalCoef = f(0.0075f, 0, "negativeThermalCoef", Comments.negativeThermalCoef);
    public final ConfigBase.ConfigFloat heatLossCoef = f(0.01f, 0, "heatLossCoef", Comments.heatLossCoef);

    @Override
    public void onLoad() {
        super.onLoad();
        Constants.neutronFluxMultiplicator = neutronFluxMultiplicator.getF();

    }

    @Override
    public void onReload() {
        super.onReload();
        Constants.neutronFluxMultiplicator = neutronFluxMultiplicator.getF();
    }

    @Override
    public @NotNull String getName() {
        return "nuclear_v2";
    }

    private static class Comments {
        static String explosion      = "activate explosion";
        static String radiationRange = "the maximum distance for radiation influence on fission, the bigger the range," +
                "the better big reactor will perform. Huge performance impact don't make it higher than 10";
        static String neutronFluxMultiplicator = " decrease it to make reactor less reactive, control how much neutron " +
                "each fission gives out (neutronFluxMultiplicator * 2.5)";
        static String negativeThermalCoef      = "increase it to decrease the temperature, make neutron less likely to impact" +
                " when temperature is higher ((temperature - 200) * negativeThermalCoef)";
        static String heatLossCoef = "Controls how much a reactor passively cools. A higher value cools it faster, a lower cools it slower.";
    }
}