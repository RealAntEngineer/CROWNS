package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class CROWNSConduction extends ConfigBase {
    public final ConfigBase.ConfigGroup conductionValues        = group(0, "conductionValues", Comments.conductionValues);
    public final ConfigFloat            heatExchangerExternal   = f(50000, 0, 100000, "heatExchangerExternal", Comments.heatExchangerExternal);
    public final ConfigFloat            heatExchangerInternal   = f(500000, 0, "heatExchangerInternal", Comments.heatExchangerInternal);
    public final ConfigInt              heatExchangerIterations = i(1, 1, "heatExchangerIterations", Comments.heatExchangerIterations);
    public final ConfigFloat            assemblyBlock           = f(50000, 0, 100000, "assemblyBlock", Comments.assemblyBlock);

    public final ConfigBase.ConfigGroup gameplay   = group(0, "gameplay", Comments.gameplay);
    public final ConfigBool             heatDamage = b(true, "heatDamage", Comments.heatDamage);

    @Override
    public String getName() {
        return "thermal";
    }

    private static class Comments {
        static String conductionValues        = "Configure the conduction coefficient";
        static String heatExchangerExternal   = "conduction coefficient between the heat exchanger and the exterior";
        static String heatExchangerInternal   = "conduction coefficient between the heat exchanger and the water flowing through it";
        static String heatExchangerIterations = "the heating of water happen by step, increase this to multiply the number of step and increase the precision (also slow down your computer of course)";
        static String assemblyBlock           = "conduction coefficient between the assembly block and the exterior";
        static String gameplay                = "gameplay config related to temperature";
        static String heatDamage              = "does heat inflict damage (too cold you freeze, too hot you're on fire)";
    }
}