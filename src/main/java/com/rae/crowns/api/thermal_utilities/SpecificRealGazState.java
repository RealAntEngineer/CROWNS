package com.rae.crowns.api.thermal_utilities;

public record SpecificRealGazState(Float temperature, Float pressure, Float specific_enthalpy, Float vaporQuality) {

    public SpecificRealGazState(Float temperature, Float pressure, Float specific_enthalpy, Float vaporQuality){
        this.temperature = Math.max(0,temperature);
        this.pressure = Math.max(0,pressure);
        this.specific_enthalpy = specific_enthalpy;
        this.vaporQuality = Math.max(0,Math.min(vaporQuality,1));
    }

    @Override
    public String toString() {
        return "SpecificRealGazState{" +
                "temperature=" + temperature +
                ", pressure=" + pressure +
                ", specific_enthalpy=" + specific_enthalpy +
                ", vaporQuality=" + vaporQuality +
                '}';
    }
}
