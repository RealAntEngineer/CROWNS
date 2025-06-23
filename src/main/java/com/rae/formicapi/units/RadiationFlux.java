package com.rae.formicapi.units;

import com.rae.formicapi.FormicApiLang;
import net.minecraft.network.chat.Component;

public enum RadiationFlux implements IUnit{
    // Assuming base unit is MBq (MegaBecquerels)
    MEGA_BECQUERELS(1f),
    CURIES(1/37000f),         // 1 Ci = 37 GBq = 37_000 MBq
    // Approximations for absorbed dose rate assuming 1 MeV per decay and full absorption in 1 kg
    GRAY_PER_SECOND(1.602e-7f),   // ~1 MeV per decay
    RAD_PER_SECOND(1.602e-5f),   // 1 Gy = 100 rad
    SIEVERT_PER_SECOND(1.602e-7f); // Q factor assumed 1

    private final float a;
    final Component symbol;

    RadiationFlux(float a) {
        this.a = a;
        this.symbol = FormicApiLang.translate("units.radiation_flux.symbol."+name().toLowerCase()).component();

    }

    public float convert(float radiationFluxInMBq) {
        return radiationFluxInMBq * a ;
    }

    public Component getSymbol() {
        return symbol;
    }
}
