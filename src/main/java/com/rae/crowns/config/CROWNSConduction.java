package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSConduction extends ConfigBase {
    public final ConfigFloat heatExchangerExternal = f(500000, 0, "heatExchangerExternal", Comments.heatExchangerExternal);
    public final ConfigFloat heatExchangerInternal = f(5000000, 0, "heatExchangerInternal", Comments.heatExchangerInternal);
    public final ConfigFloat assemblyBlock = f(50000, 0, "assemblyBlock", Comments.assemblyBlock);

    @Override
    public @NotNull String getName() {
        return "conduction";
    }

    private static class Comments {
        static @NotNull String heatExchangerExternal = "conduction coefficient between the heat exchanger and the exterior";
        static @NotNull String heatExchangerInternal = "conduction coefficient between the heat exchanger and the water flowing through it";
        static @NotNull String assemblyBlock = "conduction coefficient between the assembly block and the exterior";
    }
}