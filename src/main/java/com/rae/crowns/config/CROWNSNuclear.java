package com.rae.crowns.config;


import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSNuclear extends ConfigBase {

    public final ConfigBase.ConfigBool explosion = b(true, "explosion", Comments.explosion);
    public final ConfigBase.ConfigFloat radiationRange = f(4, 0, "radiationRange", Comments.radiationRange);
    public final ConfigBase.ConfigFloat neutronFluxMultiplicator = f(1f, 0, "neutronFluxMultiplicator", Comments.neutronFluxMultiplicator);
    public final ConfigBase.ConfigFloat negativeThermalCoef = f(0.0075f, 0, "negativeThermalCoef", Comments.negativeThermalCoef);
    public final ConfigBase.ConfigFloat heatLossCoef = f(0.01f, 0, "heatLossCoef", Comments.heatLossCoef);

    @Override
    public void onReload() {
        super.onReload();
    }

    @Override
    public @NotNull String getName() {
        return "nuclear_v2";
    }

    private static class Comments {
        static @NotNull String explosion = "activate explosion";
        static @NotNull String radiationRange = "The maximum distance neutron flux can travel, the bigger the range," +
                "the better a large reactor will perform. Huge performance impact, a value higher than 10 will cause severe lag";
        static @NotNull String neutronFluxMultiplicator = "Controls how much neutrons a fission or decay reaction emits (neutronFluxMultiplicator * 2.5)";
        static @NotNull String negativeThermalCoef = "Control the negative temperature feedback effect" +
                "((temperature - 200) * negativeThermalCoef)";
        static @NotNull String heatLossCoef = "Controls how much a reactor passively cools. A higher value cools it faster, a lower cools it slower.";
    }
}
