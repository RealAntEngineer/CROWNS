package com.rae.formicapi.units;

import com.rae.crowns.CROWNSLang;
import net.minecraft.network.chat.Component;

public enum Temperature implements IUnit{
    KELVIN(1,0, "K"),
    CELSIUS(1,-273.15f,"°C"),
    FAHRENHEIT(9f/5 , -273.15f * 9f/5 +32,"°F");

    private final float a;
    private final float b;
    private final Component symbol;

    Temperature(float a, float b, String symbol) {
        this.a = a;
        this.b = b;
        this.symbol = CROWNSLang.translate("units.temperature.symbol."+name().toLowerCase()).component();
    }

    public float convert(float kelvin) {
        return kelvin * a + b;
    }

    public Component getSymbol() {
        return symbol;
    }
}
