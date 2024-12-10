package com.rae.crowns.api.transformations;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;


public class WaterAsRealGazTransformationHelper {

    //terrible approximation just to get started
    static Float CLiquid = 4187f;
    static Float Cv = 1600f;
    static Float Cp = 2100f;
    static Float gamma = 1.3125f;

    static Float TPSat = (float) ((647 - 273) / (22.064 * 1000000 - 611));
    static Float P0 = (float) (-16000000);
    static Float T0 = (float) (372);
    static float PCrit = 22.064f * 1000000f;
    static float dh0 = 2500000f;
    static Float dhSat = (float) (-2500000f / (22.064 * 1000000 - 611));

    private static float dhVap(Float pressure) {
        return Math.max(0, dhSat * pressure + dh0);
    }

    private static float TSat(Float pressure) {
        return TPSat * pressure + T0;// take the 0 in account
    }

    private static float PSat(Float temperature) {
        return temperature / TPSat + P0;// idem
    }

    /**
     * constant pressure heating
     *
     * @param specific_heat : the heat the fluid get for each kg
     * @return the new fluid state
     */
    public static SpecificRealGazState isobaricHeating(SpecificRealGazState fluidState, float specific_heat) {
        float dh = specific_heat;
        float dx = 0;
        float dT = 0;
        if (fluidState.vaporQuality() == 0) {
            if (fluidState.temperature() + dh / CLiquid <= TSat(fluidState.pressure())) {
                dT += dh / CLiquid;
            } else {
                dT += TSat(fluidState.pressure()) - fluidState.temperature();
                if (dhVap(fluidState.pressure()) > 0) {
                    dx = (dh - dT * CLiquid) / dhVap(fluidState.pressure());
                    if (dx > 1) {
                        dx = 1;
                        dT += (dh - dhVap(fluidState.pressure()) - dT * CLiquid) / Cp;
                    }
                } else {
                    dx = 1;
                    dT += (dh - dT * CLiquid) / Cp;
                }
            }
        } else if (fluidState.vaporQuality() < 1.0) {
            dx = dh / dhVap(fluidState.pressure());
            dT = 0;
            if (dx > 1 - fluidState.vaporQuality()) {
                dx = 1 - fluidState.vaporQuality();
                //we remove the energy taken by the vaporisation
                dT = (dh - dhVap(fluidState.pressure()) * (1 - fluidState.vaporQuality())) / Cp;
            }
        } else {
            dT = dh / Cp;
        }
        return new SpecificRealGazState(
                fluidState.temperature() + dT,
                fluidState.pressure(),
                fluidState.specificEnthalpy() + dh,
                fluidState.vaporQuality() + dx);
    }

    /**
     * constant pressure heating
     *
     * @param specific_heat : the heat the fluid get for each kg
     * @return the new fluid state
     */
    public static SpecificRealGazState isobaricCooling(SpecificRealGazState fluidState, float specific_heat) {
        //TODO  verify equation
        float dh = specific_heat;
        float dx = 0;
        float dT = 0;
        if (fluidState.vaporQuality() == 1) {
            if (fluidState.temperature() + dh / Cp >= TSat(fluidState.pressure())) {
                dT += dh / Cp;
            } else {
                dT += TSat(fluidState.pressure()) - fluidState.temperature();
                if (dhVap(fluidState.pressure()) > 0) {
                    dx = (dh + dT * CLiquid) / dhVap(fluidState.pressure());
                    if (dx < -1) {
                        dx = -1;
                        dT += (dh + dhVap(fluidState.pressure()) - dT * Cp) / CLiquid;
                    }
                } else {
                    dx = -1;
                    dT += (dh - dT * Cp) / CLiquid;
                }
            }
        } else if (fluidState.vaporQuality() < 1.0) {
            dx = dh / dhVap(fluidState.pressure());
            dT = 0;
            if (dx < -fluidState.vaporQuality()) {
                dx = -fluidState.pressure();
                //we remove the energy taken by the vaporisation
                dT = (dh - dhVap(fluidState.pressure()) * (-fluidState.vaporQuality())) / CLiquid;
            }
        } else {
            dT = dh / CLiquid;
        }


        return new SpecificRealGazState(
                fluidState.temperature() + dT,
                fluidState.pressure(),
                fluidState.specificEnthalpy() - dh,
                fluidState.vaporQuality() + dx);
    }

    public static SpecificRealGazState isobaricTransfert(SpecificRealGazState fluidState, float specific_heat) {
        if (specific_heat ==0) {
            return fluidState;
        }
        else if (specific_heat >0){
            return isobaricHeating(fluidState, specific_heat);
        }
        else{
            return isobaricCooling(fluidState, specific_heat);
        }
    }

    public static float get_h(float x, float T, float P) {
        if (x == 0) {
            return (T - 273) * CLiquid;
        }
        else if(x < 1) {
            return (TSat(P) - 273) * CLiquid + dhVap(P) * x;
        }
        else{
            return (TSat(P) - 273) * CLiquid + dhVap(P) * x + (T - TSat(P)) * Cp;
        }
    }

    /**
     * adiabatic reversible expansion
     * @param fluidState
     * @param expansionFactor the initial pressure over the pressure of the fluid at the end of the turbine
     * @return the new fluid state
     */
    public static SpecificRealGazState standardExpansion(SpecificRealGazState fluidState, float expansionFactor){//vapor quality has an issue
        float Pf = fluidState.pressure() / expansionFactor;
        float dP = Pf - fluidState.pressure();
        float dT = 0;
        float dx = 0;
        if (fluidState.vaporQuality() == 1) {
            // il faut verifier que
            float Tf = (float) (Math.pow((fluidState.pressure() / Pf), ((1 - gamma) / gamma)) * fluidState.temperature());
            dT = Tf - fluidState.temperature();
            // ça marche pas
            if (Tf < TSat(Pf)) {
                dT = TSat(Pf) - fluidState.temperature();
                if (dhVap(Pf) > 0) {
                    dx = (Tf - TSat(Pf)) * Cp / dhVap(Pf);
                    if (dx < -1) {
                        dx = -1;
                    }
                }
            } else {
                dx = 1 - fluidState.vaporQuality();
            }
        }
        else if (fluidState.vaporQuality() > 0) {
            // gaz part :
            float Tf_g = (float) (Math.pow(fluidState.pressure() / Pf,(1 - gamma) / gamma) * fluidState.temperature());
            float dT_g = Tf_g - fluidState.temperature();
            float dh_g = (dT_g * Cp + dhVap(fluidState.pressure())) * fluidState.vaporQuality();
            return isobaricTransfert(new SpecificRealGazState(fluidState.temperature(),
                    Pf, get_h(0, fluidState.temperature(), Pf), 0f), dh_g);
        }
        else {
            if (fluidState.temperature() > TSat(Pf)) {
                float Pf_l = (fluidState.temperature() - T0) / TPSat;
                if (dhVap(Pf_l) == 0) {
                    return standardExpansion(new SpecificRealGazState(fluidState.temperature(), Pf_l, fluidState.specificEnthalpy(), 1f), Pf_l / Pf);
                }
                else {
                    dx = ((fluidState.temperature() - TSat(Pf)) * CLiquid) / dhVap(Pf);
                    dT = TSat(Pf) - fluidState.temperature();
                    // need to be redone
                    if (dx > 1) {
                        float Pf_v = ((TSat(Pf_l) - T0) * CLiquid - dh0) / (TPSat * CLiquid + dhSat);
                        return standardExpansion(new SpecificRealGazState(TSat(Pf_v), Pf_v, get_h(1, TSat(Pf_v), Pf_v), 1f), Pf_v / Pf);
                    }
                }
            }
        }
        return new SpecificRealGazState(
                fluidState.temperature() + dT,
                fluidState.pressure() + dP,
                get_h(fluidState.vaporQuality() + dx, fluidState.temperature() + dT, fluidState.pressure() + dP),
                fluidState.vaporQuality() + dx);
    }
    /**
     * adiabatic expansion
     * @param fluidState :
     * @param isentropicYield : how much the fluid lost enthalpy over what it should have if reversible (how much energy was taken from it)
     * @param expansionCoef : the initial pressure over the pressure of the fluid at the end of the turbine
     * @return the new fluid state
     */
    public static SpecificRealGazState standardExpansion(SpecificRealGazState fluidState, float isentropicYield, float expansionCoef){
        SpecificRealGazState revFluidState = standardExpansion(fluidState,expansionCoef);
        float reversibleDh = revFluidState.specificEnthalpy()- fluidState.specificEnthalpy();
        float losth = reversibleDh*(1-isentropicYield);
        return isobaricTransfert(revFluidState,-losth);
    }
    /**
     * adiabatic reversible compression
     * @param fluidState :
     * @param compressionFactor :the pressure of the fluid at the end of the turbine over the initial pressure
     * @return the new fluid state
     */
    public static SpecificRealGazState standardCompression(SpecificRealGazState fluidState, float compressionFactor){
        float Pf = fluidState.pressure() * compressionFactor;
        float dP = Pf - fluidState.pressure();
        float dT = 0;
        float dx = 0;
        if (fluidState.vaporQuality() == 1) {
            //il faut verifier que
            float Tf = (float) (Math.pow((fluidState.pressure() / Pf),((1 - gamma) / gamma)) * fluidState.temperature());
            dT = Tf - fluidState.temperature();
            //ça marche pas
            if (Tf < TSat(Pf)) {
                dT = TSat(Pf) - fluidState.temperature();
                if (dhVap(Pf) > 0) {
                    dx = (Tf - TSat(Pf)) * Cp / dhVap(Pf);  //-np.log(dhVap(Pf) / dhVap(Pi)) * (TPSat * Cp / dhSat)
                    if (dx < -1) {
                        dx = -1;
                    }
                } else {
                    dx = -1;
                }
            }
        }
        else if (fluidState.vaporQuality() > 0) {
            // gaz part :
            float Tf_g = (float) (Math.pow(fluidState.pressure() / Pf,(1 - gamma) / gamma) * fluidState.temperature());
            float dT_g = Tf_g - fluidState.temperature();
            float dh_g = (dT_g * Cp + dhVap(fluidState.pressure())) * fluidState.vaporQuality();

            return isobaricTransfert(new SpecificRealGazState(fluidState.temperature(), Pf, get_h(0, fluidState.temperature(), Pf), 0f), dh_g);
        }
        return new SpecificRealGazState(
                fluidState.temperature() + dT,
                fluidState.pressure() + dP,
                get_h(fluidState.vaporQuality() + dx, fluidState.temperature() + dT, fluidState.pressure() + dP),
                fluidState.vaporQuality() + dx);
    }
    /**
     * adiabatic compression
     * @param fluidState :
     * @param yield :how much the fluid gained enthalpy over what it should have if reversible (how much energy was put into it)
     * @param compressionCoef :the pressure of the fluid at the end of the turbine over the initial pressure
     * @return the new fluid state
     */
    public static SpecificRealGazState standardCompression(SpecificRealGazState fluidState, float yield, float compressionCoef){
        SpecificRealGazState revFluidState = standardExpansion(fluidState,compressionCoef);
        float reversibleDh = revFluidState.specificEnthalpy()- fluidState.specificEnthalpy();
        float losth = reversibleDh*(1-yield);
        return isobaricTransfert(revFluidState,-losth);
    }


    public static SpecificRealGazState mix(SpecificRealGazState first, float firstAmount, SpecificRealGazState second, float secondAmount){
        float T = first.temperature()*firstAmount/(firstAmount+ secondAmount) + second.temperature()*secondAmount/(firstAmount+ secondAmount);
        float P = first.pressure()*firstAmount/(firstAmount+ secondAmount) + second.pressure()*secondAmount/(firstAmount+ secondAmount);
        float x = first.vaporQuality()*firstAmount/(firstAmount+ secondAmount) + second.vaporQuality()*secondAmount/(firstAmount+ secondAmount);
        return new SpecificRealGazState(
                T, P, WaterAsRealGazTransformationHelper.get_h(T,P,x), x
        );
    }
}
