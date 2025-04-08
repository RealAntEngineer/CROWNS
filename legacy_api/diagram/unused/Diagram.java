package com.rae.crowns.api.diagram.unused;

public interface Diagram {
    IsoStateFunction isothermic(float temperature);
    IsoStateFunction isochoric(float specificVolume);
    IsoStateFunction isobaric(float pressure);
    IsoStateFunction isenthalpic(float enthalpy);

    float Interpolate();
}