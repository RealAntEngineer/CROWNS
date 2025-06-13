package com.rae.colony_api.thermal_utilities;


import com.rae.colony_api.math.Solvers;

import java.util.Arrays;
import java.util.function.Function;

//TODO make an interface.
public class WaterCubicEOSTransformationHelper {

    private static final PengRobinsonEOS EOS = new PengRobinsonEOS(647.1, 22.064e6,0.344);

    public static final SpecificRealGazState DEFAULT_STATE = new SpecificRealGazState(300f, 101300f, get_h(0,300,101300),0f);

    public static SpecificRealGazState isobaricTransfer(SpecificRealGazState fluidState, float specific_heat) {
        if (specific_heat == 0) {
            return fluidState;
        }
        else {
            float newH = fluidState.specificEnthalpy() + specific_heat;
            float newPressure = fluidState.pressure();
            float newT = get_T(newPressure,newH);
            float newVaporQuality = get_x(newH,newT,newPressure);
            return new SpecificRealGazState(newT, newPressure, newH, newVaporQuality);
        }
    }

    public static float get_h(float x, float T, float P) {
        double[] roots = EOS.getZFactors(T, P);
        if (roots.length < 2) throw new IllegalArgumentException("Not in coexistence region");

        double Zl = Arrays.stream(roots).min().getAsDouble();
        double Zv = Arrays.stream(roots).max().getAsDouble();

        double R = PengRobinsonEOS.R;
        double Vl = Zl * R * T / P;
        double Vv = Zv * R * T / P;

        double hl = EOS.totalEnthalpy(T, Vl);
        double hv = EOS.totalEnthalpy(T, Vv);

        return (float)((1 - x) * hl + x * hv);
    }

    public static float get_T(float P, float h) {
        float Tmin = 273f;
        float Tmax = 800f;
        float epsilon = 0.1f;

        // Define residual: f(T) = h(T) - h_target
        Function<Float, Float> residual = (Float Tguess) -> {
            try {
                double[] roots = EOS.getZFactors(Tguess, P);
                if (roots.length == 1) {
                    // Single phase
                    double Z = roots[0];
                    double Vm = Z * PengRobinsonEOS.R * Tguess / P;
                    double hComputed = EOS.totalEnthalpy(Tguess, Vm);
                    return (float)(hComputed - h);
                } else if (roots.length >= 2) {
                    double Zl = Arrays.stream(roots).min().getAsDouble();
                    double Zv = Arrays.stream(roots).max().getAsDouble();

                    double R = PengRobinsonEOS.R;
                    double Vl = Zl * R * Tguess / P;
                    double Vv = Zv * R * Tguess / P;

                    double hl = EOS.totalEnthalpy(Tguess, Vl);
                    double hv = EOS.totalEnthalpy(Tguess, Vv);

                    // If h within [hl, hv], calculate quality and mixture enthalpy
                    if (h >= hl && h <= hv) {
                        return 0f; // found T: h is between saturated enthalpies
                    }

                    // Otherwise, pick closest pure-phase approximation
                    double hClosest = (Math.abs(h - hl) < Math.abs(h - hv)) ? hl : hv;
                    return (float)(hClosest - h);
                }
            } catch (Exception e) {
                return Float.NaN;
            }
            return Float.NaN;
        };

        float T = Solvers.dichotomy(residual, Tmin, Tmax, epsilon);
        if (Float.isNaN(T)) throw new RuntimeException("No solution for T at given h and P");

        return T;
    }
    public static float get_x(float h, float T, float P) {
        double[] roots = EOS.getZFactors(T, P);
        if (roots.length < 2) throw new IllegalArgumentException("Not in coexistence region");

        double Zl = Arrays.stream(roots).min().getAsDouble();
        double Zv = Arrays.stream(roots).max().getAsDouble();

        double R = PengRobinsonEOS.R;
        double Vl = Zl * R * T / P;
        double Vv = Zv * R * T / P;

        double hl = EOS.totalEnthalpy(T, Vl);
        double hv = EOS.totalEnthalpy(T, Vv);

        return (float)((h - hl) / (hv - hl));
    }

    /**
     * adiabatic reversible expansion
     * @param initial
     * @param expansionFactor the initial pressure over the pressure of the fluid at the end of the turbine
     * @return the new fluid state
     */
    public static SpecificRealGazState isentropicExpansion(SpecificRealGazState initial, float expansionFactor) {
        float T1 = initial.temperature();
        float P1 = initial.pressure();
        float finalPressure = P1/expansionFactor;

        // Step 1: Compute initial molar volume and entropy
        double[] roots1 = EOS.getZFactors(T1, P1);
        if (roots1.length == 0) throw new RuntimeException("No Z root at initial state");
        double Z1 = Arrays.stream(roots1).max().getAsDouble(); // assume vapor
        double Vm1 = Z1 * PengRobinsonEOS.R * T1 / P1;
        double sTarget = EOS.totalEntropy(T1, Vm1);

        // Step 2: Find T2 such that s(T2, P2) = s1
        float Tmin = 200f;
        float Tmax = 1500f;
        float epsilon = 0.1f;

        Function<Float, Float> entropyError = (Float Tguess) -> {
            try {
                double[] roots2 = EOS.getZFactors(Tguess, finalPressure);
                if (roots2.length == 0) return Float.NaN;

                double Z2 = Arrays.stream(roots2).max().getAsDouble(); // assume vapor
                double Vm2 = Z2 * PengRobinsonEOS.R * Tguess / finalPressure;
                double s2 = EOS.totalEntropy(Tguess, Vm2);

                return (float)(s2 - sTarget);
            } catch (Exception e) {
                return Float.NaN;
            }
        };

        float T2 = Solvers.dichotomy(entropyError, Tmin, Tmax, epsilon);
        if (Float.isNaN(T2)) throw new RuntimeException("Failed to find T for isentropic compression");

        // Step 3: Compute h2 and build the new state
        double[] roots2 = EOS.getZFactors(T2, finalPressure);
        double Z2 = Arrays.stream(roots2).max().getAsDouble(); // again, assume vapor
        double Vm2 = Z2 * PengRobinsonEOS.R * T2 / finalPressure;
        float h2 = (float) EOS.totalEnthalpy(T2, Vm2);

        return new SpecificRealGazState(T2, finalPressure, h2, get_x(h2, T2, finalPressure));
    }
    /**
     * adiabatic expansion
     * @param fluidState :
     * @param isentropicYield : how much the fluid lost enthalpy over what it should have if reversible (how much energy was taken from it)
     * @param expansionCoef : the initial pressure over the pressure of the fluid at the end of the turbine
     * @return the new fluid state
     */
    public static SpecificRealGazState realExpansion(SpecificRealGazState fluidState, float isentropicYield, float expansionCoef){
        SpecificRealGazState revFluidState = isentropicExpansion(fluidState,expansionCoef);
        float reversibleDh = revFluidState.specificEnthalpy()- fluidState.specificEnthalpy();
        float losth = reversibleDh*(1-isentropicYield);
        return isobaricTransfer(revFluidState,-losth);
    }
    /**
     * adiabatic reversible compression
     * @param initial :
     * @param compressionFactor :the pressure of the fluid at the end of the turbine over the initial pressure
     * @return the new fluid state
     */
    public static SpecificRealGazState isentropicCompression(SpecificRealGazState initial, float compressionFactor) {
        float T1 = initial.temperature();
        float P1 = initial.pressure();
        float finalPressure = P1*compressionFactor;

        // Step 1: Compute initial molar volume and entropy
        double[] roots1 = EOS.getZFactors(T1, P1);
        if (roots1.length == 0) throw new RuntimeException("No Z root at initial state");
        double Z1 = Arrays.stream(roots1).max().getAsDouble(); // assume vapor
        double Vm1 = Z1 * PengRobinsonEOS.R * T1 / P1;
        double sTarget = EOS.totalEntropy(T1, Vm1);

        // Step 2: Find T2 such that s(T2, P2) = s1
        float Tmin = 200f;
        float Tmax = 1500f;
        float epsilon = 0.1f;

        Function<Float, Float> entropyError = (Float Tguess) -> {
            try {
                double[] roots2 = EOS.getZFactors(Tguess, finalPressure);
                if (roots2.length == 0) return Float.NaN;

                double Z2 = Arrays.stream(roots2).max().getAsDouble(); // assume vapor
                double Vm2 = Z2 * PengRobinsonEOS.R * Tguess / finalPressure;
                double s2 = EOS.totalEntropy(Tguess, Vm2);

                return (float)(s2 - sTarget);
            } catch (Exception e) {
                return Float.NaN;
            }
        };

        float T2 = Solvers.dichotomy(entropyError, Tmin, Tmax, epsilon);
        if (Float.isNaN(T2)) throw new RuntimeException("Failed to find T for isentropic compression");

        // Step 3: Compute h2 and build the new state
        double[] roots2 = EOS.getZFactors(T2, finalPressure);
        double Z2 = Arrays.stream(roots2).max().getAsDouble(); // again, assume vapor
        double Vm2 = Z2 * PengRobinsonEOS.R * T2 / finalPressure;
        float h2 = (float) EOS.totalEnthalpy(T2, Vm2);

        return new SpecificRealGazState(T2, finalPressure, h2, get_x(h2, T2, finalPressure));
    }

    /**
     * adiabatic compression
     * @param fluidState :
     * @param yield :how much the fluid gained enthalpy over what it should have if reversible (how much energy was put into it)
     * @param compressionCoef :the pressure of the fluid at the end of the turbine over the initial pressure
     * @return the new fluid state
     */
    public static SpecificRealGazState realCompression(SpecificRealGazState fluidState, float yield, float compressionCoef){
        SpecificRealGazState revFluidState = isentropicCompression(fluidState,compressionCoef);
        float reversibleDh = revFluidState.specificEnthalpy()- fluidState.specificEnthalpy();
        float losth = reversibleDh*(1-yield);
        return isobaricTransfer(revFluidState,-losth);
    }

    //TODO -> it seems to not be working when amount are too low -> protection against 0 values ?
    public static SpecificRealGazState mix(SpecificRealGazState first, float firstAmount, SpecificRealGazState second, float secondAmount){
        if (firstAmount == 0) return second;
        if (secondAmount == 0) return first;
        float P = first.pressure()*firstAmount/(firstAmount+ secondAmount) + second.pressure()*secondAmount/(firstAmount+ secondAmount);
        float h = first.specificEnthalpy()*firstAmount/(firstAmount+ secondAmount) + second.specificEnthalpy()*secondAmount/(firstAmount+ secondAmount);
        //float x = first.vaporQuality()*firstAmount/(firstAmount+ secondAmount) + second.vaporQuality()*secondAmount/(firstAmount+ secondAmount);
        SpecificRealGazState state = new SpecificRealGazState(
                get_T(P,h), P, h, get_x(h,get_T(P,h),P));
        //System.out.println(state);
        if (first.temperature().isNaN() || second.temperature().isNaN()){
            return DEFAULT_STATE;
        }
        if (first.temperature() > 20000 || second.temperature() > 20000){
            return DEFAULT_STATE;
        }
        return state;
    }

}
