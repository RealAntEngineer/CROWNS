package com.rae.crowns.api.transformations;

import com.rae.crowns.api.thermal_utilities.SpecificFluidState;

public class WaterAsRealGazTransformationHelper {
    static Float CLiquid = 4187f;
    static Float Cv = 1600f;
    static Float Cp = 2100f;


    /**
     * constant pressure heating
     * @param specific_heat : the heat the fluid get for each kg
     * @return the new fluid state
     */
    public static SpecificFluidState isobaricHeating(SpecificFluidState fluidState, float specific_heat){
        Float dh = specific_heat;
        Float dT = dh/Cp;
        Float dV = 0f;
        Float dS = 0f;
        Float dP = 0f;
        boolean isLiquid = true;
        if (isLiquid){
            dT = dh/CLiquid;
            dP = 0f;
        }

        return new SpecificFluidState(
                fluidState.temperature()+ dT,
                fluidState.pressure(),
                fluidState.specific_enthalpy()+dh,
                fluidState.specific_volume()+dV,
                fluidState.specific_volume()+dS);
    }

    /**
     * constant volume heating
     * @param specific_heat : the heat the fluid get for each kg
     * @return the new fluid state
     */
    //warning do not use !!!
    public static SpecificFluidState standardHeating(SpecificFluidState fluidState, float specific_heat){
        Float dh = specific_heat;
        return new SpecificFluidState(
                null,
                null,
                fluidState.specific_enthalpy()+dh,
                fluidState.specific_volume(),
                null);
    }
    /**
     * constant pressure heating
     * @param specific_heat : the heat the fluid get for each kg
     * @return the new fluid state
     */
    public static SpecificFluidState isobaricCooling(SpecificFluidState fluidState, float specific_heat){
        Float dh = - specific_heat;
        return new SpecificFluidState(
                null,
                fluidState.pressure(),
                fluidState.specific_enthalpy()+dh,
                null,
                null);
    }

    /**
     * constant volume cooling
     * @param fluidState :
     * @param specific_heat : the heat the fluid loose for each kg
     * @return the new fluid state
     */
    //warning do not use !!!
    public static SpecificFluidState standardCooling(SpecificFluidState fluidState, float specific_heat){
        Float dh = - specific_heat;
        return new SpecificFluidState(
                null,
                null,
                fluidState.specific_enthalpy()+dh,
                fluidState.specific_volume(),//volume is constant
                null );
    }

    /**
     * adiabatic reversible expansion
     * @param fluidState :
     * @param expansionCoef : the initial pressure over the pressure of the fluid at the end of the turbine
     * @return the new fluid state
     */
    public static SpecificFluidState standardExpansion(SpecificFluidState fluidState, float expansionCoef){
        Float initialPressure = fluidState.pressure();
        Float finalPressure = initialPressure/expansionCoef;
        Float ds = 0f; // reversible ?

        return new SpecificFluidState(
                null,
                finalPressure,
                null,
                null,
                fluidState.specific_entropy() + ds);
    }
    /**
     * adiabatic expansion
     * @param fluidState :
     * @param isentropicYield : how much the fluid lost enthalpy over what it should have if reversible (how much energy was taken from it)
     * @param expansionCoef : the initial pressure over the pressure of the fluid at the end of the turbine
     * @return the new fluid state
     */
    public static SpecificFluidState standardExpansion(SpecificFluidState fluidState,float isentropicYield, float expansionCoef){
        SpecificFluidState revFluidState = standardExpansion(fluidState,expansionCoef);
        float reversibleDh = revFluidState.specific_enthalpy()- fluidState.specific_enthalpy();
        Float realDh = reversibleDh * isentropicYield;
        return new SpecificFluidState(
                null,
                fluidState.pressure(),
                fluidState.specific_enthalpy()+realDh,
                null,
                null );
    }
    /**
     * adiabatic reversible compression
     * @param fluidState :
     * @param compressionCoef :the pressure of the fluid at the end of the turbine over the initial pressure
     * @return the new fluid state
     */
    public static SpecificFluidState standardCompression(SpecificFluidState fluidState, float compressionCoef){
        Float initialPressure = fluidState.pressure();
        Float finalPressure = initialPressure*compressionCoef;
        return new SpecificFluidState(
                null,
                finalPressure,
                null,
                null,
                fluidState.specific_entropy());
    }
    /**
     * adiabatic compression
     * @param fluidState :
     * @param yield :how much the fluid gained enthalpy over what it should have if reversible (how much energy was put into it)
     * @param compressionCoef :the pressure of the fluid at the end of the turbine over the initial pressure
     * @return the new fluid state
     */
    public static SpecificFluidState standardCompression(SpecificFluidState fluidState, float yield, float compressionCoef){
        SpecificFluidState revFluidState = standardCompression(fluidState,compressionCoef);
        float reversibleDh = revFluidState.specific_enthalpy()- fluidState.specific_enthalpy();
        // yield is a little more complicated in compression no ?
        Float realDh = reversibleDh * 1 / yield;
        return new SpecificFluidState(
                null,
                fluidState.pressure(),
                fluidState.specific_enthalpy()+realDh,
                null,
                null );
    }
}
