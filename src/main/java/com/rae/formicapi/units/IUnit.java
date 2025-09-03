package com.rae.formicapi.units;

import net.minecraft.network.chat.Component;

/**
 * this is an interface meant as a template for Enums representing units
 */
public interface IUnit {
    /**
     *
     * @param mainUnitValue value in the unit used as main
     * @return converted value
     */
    float convert(float mainUnitValue);

     Component getSymbol();
}
