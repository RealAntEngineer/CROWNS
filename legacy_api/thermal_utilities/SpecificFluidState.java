package com.rae.crowns.api.thermal_utilities;

public record SpecificFluidState(Float temperature, Float pressure, Float specific_enthalpy, Float specific_volume, Float specific_entropy) {
    public static String[] header = {"temperature","pressure","specific_enthalpy","specific_volume","specific_entropy"};


    public static SpecificFluidState empty() {
        return new SpecificFluidState(null,null,null,null,null);
    }
}
