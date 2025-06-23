package com.rae.formicapi.thermal_utilities;

import com.rae.formicapi.thermal_utilities.eos.PengRobinsonEOS;
import com.rae.formicapi.thermal_utilities.eos.VanDerWaalsEOS;

public class EOSLibrary {
    //you can find a,b values at https://en.wikipedia.org/wiki/Van_der_Waals_constants_(data_page)
    public static PengRobinsonEOS getPRWaterEOS() {
        double Tc = 647.1;//critical temperature in Kelvin
        double Pc = 22.064e6;//critical pressure in Pascals
        double omega = 0.344;//omega.... What is it exactly ?
        double M = 18.01528e-3f;
        return new PengRobinsonEOS(Tc, Pc, omega, M);
    }

    public static VanDerWaalsEOS getVanDerWaalsWaterEOS() {
        double Tc = 647.1;
        double Pc = 22.064e6;

        return new VanDerWaalsEOS(Pc, Tc);
    }
}