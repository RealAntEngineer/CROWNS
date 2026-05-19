package com.rae.crowns.config;


import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSKinetics extends ConfigBase {

    //public CROWNSStress stressValues  = nested(0, CROWNSStress::new, Comments.stress);
    public final ConfigBase.ConfigGroup turbineValues           = group(0, "turbineValues", Comments.turbineStage);
    public final ConfigBase.ConfigFloat turbineCoefficient      = f(1, 0, "turbineCoefficient", Comments.turbineCoefficient);
    public final ConfigBase.ConfigFloat turbineIsentropicYield  = f(1, 0, 1, "turbineIsentropicYield", Comments.turbineIsentropicYield);
    public final ConfigBase.ConfigFloat turbineLowPressureBound = f(2e3f, 0, "turbineLowPressureBound");
    public final ConfigBase.ConfigInt   turbineSpeed            = i(256, 1, "turbineSpeed", Comments.turbineSpeed);


    public final ConfigBase.ConfigGroup compressorValues          = group(0, "compressorValues", Comments.compressorValues);
    public final ConfigBase.ConfigFloat compressorIsentropicYield = f(1, 0, 1, "turbineIsentropicYield", Comments.compressorIsentropicYield);
    public final ConfigBase.ConfigFloat compressorFlowRef         = f(512, 1, "compressorFlowRef", Comments.compressorFlowRef);
    public final ConfigBase.ConfigFloat compressorPressureRef     = f(1e6f, 1, "compressorPressureRef", Comments.compressorPressureRef);
    public final ConfigBase.ConfigFloat compressorSpeedRef        = f(64, 1, "compressorSpeedRef", Comments.compressorSpeedRef);


    @Override
    public @NotNull String getName() {
        return "kinetics";
    }

    private static class Comments {
        static @NotNull String turbineStage              = "Fine tune the parameters of turbine stages";
        static @NotNull String turbineCoefficient        = "turbine capacity factor";
        static @NotNull String turbineIsentropicYield    = "turbine yield";
        static @NotNull String turbineSpeed              = "turbine speed";
        static @NotNull String compressorValues          = "Fine tune the parameters of the compressor";
        static @NotNull String compressorIsentropicYield = "compressor yield";
        static @NotNull String compressorFlowRef         = "q_ref in : deltaP = P_ref * (w/w_ref)**2 * (1- (q/q_ref)**2)";
        static @NotNull String compressorPressureRef     = "P_ref in : deltaP = P_ref * (w/w_ref)**2 * (1- (q/q_ref)**2)";
        static @NotNull String compressorSpeedRef        = "w_ref in : deltaP = P_ref * (w/w_ref)**2 * (1- (q/q_ref)**2)";

    }
}
