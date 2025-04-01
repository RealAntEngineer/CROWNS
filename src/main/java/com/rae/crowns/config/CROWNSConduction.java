package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSConduction extends ConfigBase {
    public final ConfigFloat heatExchangerExternal = f(10000,0,"heatExchangerExternal", Comments.heatExchangerExternal);
    public final ConfigFloat heatExchangerInternal = f(1000,0,"heatExchangerInternal", Comments.heatExchangerInternal);
    public final ConfigFloat assemblyBlock = f(10000,0,"assemblyBlock", Comments.assemblyBlock);
    @Override
    public @NotNull String getName() {
        return "conduction";
    }
    private static class Comments {
        static String heatExchangerExternal ="conduction coefficient between the heat exchanger and the exterior";
        static String heatExchangerInternal ="conduction coefficient between the heat exchanger and the water flowing through it";
        static String assemblyBlock = "conduction coefficient between the assembly block and the exterior";
    }
}
