package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSConduction extends ConfigBase {
    public final ConfigFloat heatExchangerExternal = f(50000, 0, 100000, "heatExchangerExternal", Comments.heatExchangerExternal);
    public final ConfigFloat heatExchangerInternal = f(500000, 0, "heatExchangerInternal", Comments.heatExchangerInternal);
    public final ConfigInt heatExchangerIterations = i(1, 1, "heatExchangerIterations", Comments.heatExchangerIterations);
    public final ConfigFloat assemblyBlock = f(50000, 0, 100000, "assemblyBlock", Comments.assemblyBlock);

    @Override
    public @NotNull String getName() {
        return "conduction";
    }

    private static class Comments {
        static @NotNull String heatExchangerExternal = "conduction coefficient between the heat exchanger and the exterior";
        static @NotNull String heatExchangerInternal = "conduction coefficient between the heat exchanger and the water flowing through it";
        static @NotNull String heatExchangerIterations = "the heating of water happen by step, increase this to multiply the number of step and increase the precision (also slow down your computer of course)";
        static @NotNull String assemblyBlock = "conduction coefficient between the assembly block and the exterior";
    }
}