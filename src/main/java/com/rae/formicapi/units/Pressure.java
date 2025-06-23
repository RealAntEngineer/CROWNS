package com.rae.formicapi.units;

import com.rae.formicapi.FormicApiLang;
import net.minecraft.network.chat.Component;

public enum Pressure implements IUnit {
    PASCALS(1),
    BAR(1f/100000),
    ATMOSPHERES(1f/101300),
    PSI(1f/6895);

    private final float a;
    final Component symbol;

    Pressure(float a) {
        this.a = a;
        this.symbol = FormicApiLang.translate("units.pressure.symbol."+name().toLowerCase()).component();
    }

    public float convert(float pascal) {
        return pascal * a;
    }

    public Component getSymbol() {
        return symbol;
    }
}
