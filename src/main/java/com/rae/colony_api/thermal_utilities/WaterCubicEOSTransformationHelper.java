package com.rae.colony_api.thermal_utilities;


import com.rae.colony_api.math.Solvers;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Function;

//TODO make an interface.
public class WaterCubicEOSTransformationHelper {

    private static final PengRobinsonEOS EOS = new PengRobinsonEOS(647.1, 22.064e6,0.344);
    private static final double M = 18.01528e-3f;
    private static final double R = PengRobinsonEOS.R / M;

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

    /**
     * Computes enthalpy by mass for a given vapor quality x, temperature T, and pressure P.
     * @param x vapor fraction (0 = liquid, 1 = vapor)
     * @param T temperature in K
     * @param P pressure in Pa
     * @return specific enthalpy in J/kg
     */
    public static float get_h(float x, float T, float P) {
        final double R = PengRobinsonEOS.R;

        double[] roots = EOS.getZFactors(T, P);

        if (x <= 0f) {
            // Pure liquid
            double Zl = Arrays.stream(roots).min().orElseGet(() -> roots[0]); // best effort
            double Vl = Zl * R * T / P;
            double hl = EOS.totalEnthalpy(T, Vl);
            return (float) (hl * M);
        }

        if (x >= 1f) {
            // Pure vapor
            double Zv = Arrays.stream(roots).max().orElseGet(() -> roots[0]); // best effort
            double Vv = Zv * R * T / P;
            double hv = EOS.totalEnthalpy(T, Vv);
            return (float) (hv * M);
        }

        // Two-phase region expected
        if (roots.length < 2) {
            throw new IllegalArgumentException("Not in two-phase region: cannot compute h with quality " + x);
        }

        double Zl = Arrays.stream(roots).min().getAsDouble();
        double Zv = Arrays.stream(roots).max().getAsDouble();

        double Vl = Zl * R * T / P;
        double Vv = Zv * R * T / P;

        double hl = EOS.totalEnthalpy(T, Vl);
        double hv = EOS.totalEnthalpy(T, Vv);

        double hMix = (1.0 - x) * hl + x * hv;
        return (float) (hMix * M);
    }

    public static float get_T(float P, float h) {
        //change that to a gradient decent + ensure we are using a by mass enthalpy and not a molar enthalpy
        float Tmin = 273f;
        float epsilon = 0.1f;

        // Define residual: f(T) = h(T) - h_target
        Function<Float, Float> residual = (Float Tguess) -> {
            try {
                double[] roots = EOS.getZFactors(Tguess, P);
                if (roots.length == 1) {
                    // Single phase
                    double Z = roots[0];
                    double Vm = Z * PengRobinsonEOS.R * Tguess / P;
                    double hComputed = EOS.totalEnthalpy(Tguess, Vm) * M;//this is a molar enthalpy we need a by mass
                    return (float) Math.abs(hComputed - h);
                } else if (roots.length >= 2) {
                    double Zl = Arrays.stream(roots).min().getAsDouble();
                    double Zv = Arrays.stream(roots).max().getAsDouble();
                    double Vl = Zl * R * Tguess / P;
                    double Vv = Zv * R * Tguess / P;

                    double hl = EOS.totalEnthalpy(Tguess, Vl) * M;
                    double hv = EOS.totalEnthalpy(Tguess, Vv) * M;

                    // If h within [hl, hv], calculate quality and mixture enthalpy
                    if (h >= hl && h <= hv) {
                        return 0f; // found T: h is between saturated enthalpies
                    }

                    // Otherwise, pick closest pure-phase approximation
                    double hClosest = (Math.abs(h - hl) < Math.abs(h - hv)) ? hl : hv;
                    return (float)Math.abs(hClosest - h);
                }
            } catch (Exception e) {
                return Float.MAX_VALUE;
            }
            return Float.MAX_VALUE;
        };

        float T = Solvers.gradientDecent(residual, Tmin, epsilon, 0.01f);
        if (Float.isNaN(T)) throw new RuntimeException("No solution for T at given h and P");

        return T;
    }
    /**
     * Estimate vapor quality `x` from enthalpy at given T and P.
     * Returns:
     *   - 0 for pure liquid
     *   - 1 for pure vapor
     *   - in (0, 1) for two-phase mixture
     */
    public static float get_x(float h, float T, float P) {
        double[] roots = EOS.getZFactors(T, P);
        if (roots.length < 2) {
            // Not in coexistence region → determine if it's clearly vapor or liquid
            if (roots.length == 1) {
                double Z = roots[0];
                double V = Z * PengRobinsonEOS.R * T / P;
                double hSingle = EOS.totalEnthalpy(T, V);
                return (float) ((h > hSingle) ? 1.0 : 0.0); // crude guess
            }
            throw new IllegalArgumentException("Not in coexistence region and cannot infer phase");
        }
        double Zl = Arrays.stream(roots).min().getAsDouble();
        double Zv = Arrays.stream(roots).max().getAsDouble();

        double R = PengRobinsonEOS.R;
        double Vl = Zl * R * T / P;
        double Vv = Zv * R * T / P;

        double hl = EOS.totalEnthalpy(T, Vl);
        double hv = EOS.totalEnthalpy(T, Vv);

        double denominator = hv - hl;
        if (Math.abs(denominator) < 1e-6) {
            // Avoid divide-by-zero
            return (float)((h > hl) ? 1.0 : 0.0);
        }

        float x = (float)((h - hl) / denominator);

        // Clamp to [0,1] to account for numerical error
        if (x < 0f) return 0f;
        if (x > 1f) return 1f;
        return x;
    }
    /**
     * Estimate vapor quality `x` from entropy at given T and P.
     * Returns:
     *   - 0 for pure liquid
     *   - 1 for pure vapor
     *   - in (0, 1) for two-phase mixture
     */
    public static float get_x_from_entropy(double s, float T, float P) {
        double[] roots = EOS.getZFactors(T, P);

        if (roots.length < 2) {
            // Single-phase region: decide based on entropy difference
            double Z = roots[0];
            double Vm = Z * PengRobinsonEOS.R * T / P;
            double sPhase = EOS.totalEntropy(T, Vm);

            // Estimate which side: vapor has much higher entropy
            // You can also use saturation entropy if needed
            return s > sPhase ? 1.0f : 0.0f;
        }

        // Two-phase region
        double Zl = Arrays.stream(roots).min().getAsDouble();
        double Zv = Arrays.stream(roots).max().getAsDouble();

        double Vl = Zl * PengRobinsonEOS.R * T / P;
        double Vv = Zv * PengRobinsonEOS.R * T / P;

        double sl = EOS.totalEntropy(T, Vl);
        double sv = EOS.totalEntropy(T, Vv);

        float x = (float) ((s - sl) / (sv - sl));
        return Math.max(0.0f, Math.min(1.0f, x)); // Clamp to [0,1]
    }
    private static @NotNull SpecificRealGazState isentropicPressureChange(float T1, float P1, float finalPressure) {
        // Step 1: Initial entropy
        double[] roots1 = EOS.getZFactors(T1, P1);
        if (roots1.length == 0) throw new RuntimeException("No Z root at initial state");

        double Z1 = Arrays.stream(roots1).max().getAsDouble(); // assume vapor for expansion
        double Vm1 = Z1 * PengRobinsonEOS.R * T1 / P1;
        double sTarget = EOS.totalEntropy(T1, Vm1);

        // Step 2: Find T2 such that s(T2, P2) = s1
        Function<Float, Float> entropyError = (Float Tguess) -> {
            try {
                double[] roots2 = EOS.getZFactors(Tguess, finalPressure);
                if (roots2.length == 0) return Float.MAX_VALUE;

                if (roots2.length >= 2) {
                    // Two-phase region: compute s_mix
                    double Zl = Arrays.stream(roots2).min().getAsDouble();
                    double Zv = Arrays.stream(roots2).max().getAsDouble();
                    double Vl = Zl * PengRobinsonEOS.R * Tguess / finalPressure;
                    double Vv = Zv * PengRobinsonEOS.R * Tguess / finalPressure;
                    double sl = EOS.totalEntropy(Tguess, Vl);
                    double sv = EOS.totalEntropy(Tguess, Vv);

                    // Estimate x from entropy balance
                    float xGuess = (float) ((sTarget - sl) / (sv - sl));
                    if (xGuess < 0 || xGuess > 1) return Float.MAX_VALUE;

                    double sMix = (1.0 - xGuess) * sl + xGuess * sv;
                    return (float) Math.abs(sMix - sTarget);
                } else {
                    // Single-phase vapor or liquid
                    double Z2 = roots2[0];
                    double Vm2 = Z2 * PengRobinsonEOS.R * Tguess / finalPressure;
                    double s2 = EOS.totalEntropy(Tguess, Vm2);
                    return (float) Math.abs(s2 - sTarget);
                }
            } catch (Exception e) {
                return Float.MAX_VALUE;
            }
        };

        // You could tighten bounds depending on the expected fluid range
        float T2 = Solvers.gradientDecent(entropyError, T1, 0.5f, 0.05f);
        if (Float.isNaN(T2)) throw new RuntimeException("Failed to find T for isentropic expansion");

        // Step 3: Get phase info and build final state
        double[] roots2 = EOS.getZFactors(T2, finalPressure);
        float x, h;

        if (roots2.length >= 2) {
            // Two-phase: compute x and h from mixture
            x = get_x_from_entropy(sTarget, T2, finalPressure);
            h = get_h(x, T2, finalPressure);
        } else {
            // Single-phase
            double Z2 = roots2[0];
            double Vm2 = Z2 * PengRobinsonEOS.R * T2 / finalPressure;
            h = (float) ( EOS.totalEnthalpy(T2, Vm2) * M);
            x = (float) (Z2 > 0.8 ? 1.0 : 0.0); // crude heuristic
        }

        return new SpecificRealGazState(T2, finalPressure, h, x);
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
        float finalPressure = P1 / expansionFactor;

        return isentropicPressureChange(T1, P1, finalPressure);
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

        return isentropicPressureChange(T1, P1, finalPressure);

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
