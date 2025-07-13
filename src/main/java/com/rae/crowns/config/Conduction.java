package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class Conduction extends ConfigBase {
    public final ConfigBool limitConduction = b(true, "limitConduction", Comments.limitConduction);
    public final ConfigInt conductionLimitDistance = i(2, 1, "conductionLimit", Comments.conductionLimitDistance);
    public final ConfigFloat heatExchangerExternal = f(50000,0,"heatExchangerExternal", Comments.heatExchangerExternal);
    public final ConfigFloat heatExchangerInternal = f(50000,0,"heatExchangerInternal", Comments.heatExchangerInternal);
    public final ConfigFloat assemblyBlock = f(50000,0,"assemblyBlock", Comments.assemblyBlock);
    @Override
    public @NotNull String getName() {
        return "conduction";
    }
    private static class Comments {
        static String limitConduction ="limit the range of conduction to only a few chunks around block entity that have temperature";
        static String conductionLimitDistance ="the number of section pos";
        static String heatExchangerExternal ="conduction coefficient between the heat exchanger and the exterior";
        static String heatExchangerInternal ="conduction coefficient between the heat exchanger and the water flowing through it";
        static String assemblyBlock = "conduction coefficient between the assembly block and the exterior";
    }
}
