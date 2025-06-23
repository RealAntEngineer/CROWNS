package com.rae.formicapi.thermal_utilities.helper;

import com.rae.formicapi.math.Solvers;
import com.rae.formicapi.thermal_utilities.eos.PengRobinsonEOS;
import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import net.createmod.catnip.data.Couple;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static com.rae.formicapi.thermal_utilities.eos.EquationOfState.R;

//TODO make an interface.
public class WaterCubicEOS {
    private static final PengRobinsonEOS EOS = new PengRobinsonEOS(647.1, 22.064e6,0.344,18.01528e-3f);

    public static final SpecificRealGazState DEFAULT_STATE = new SpecificRealGazState(300f, 101300f, (float) EOS.getEnthalpy(300f,101300f,0f),0f);




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
    //TODO test it. It probably can be improved
    /**
     * Computes enthalpy by mass for a given vapor quality x, temperature T, and pressure P.
     * @param x vapor fraction (0 = liquid, 1 = vapor)
     * @param T temperature in K
     * @param P pressure in Pa
     * @return specific enthalpy in J/kg
     */
    public static float get_h(float x, float T, float P) {
        return (float) EOS.getEnthalpy(T, P, x);
    }
    //to get the temperature, we need to make a call to the
    public static float get_T(float P, float h) {
        //change that to a gradient decent + ensure we are using a by mass enthalpy and not a molar enthalpy
        //this doesn't work -> it gives weird results
        // Define residual: f(T) = h(T) - h_target
        // Step 1: Find T_sat by inverting computeSaturationPressure
        Float x = null;
        try {
            float T_sat = (float) EOS.saturationTemperature(P); // root-finding on computeSaturationPressure(T) - P

            // Step 2: Compute saturated enthalpies
            double h_l = get_h(0.0f, T_sat, P);
            double h_v = get_h(1.0f, T_sat, P);
            if (h >= h_l && h <= h_v) {
                // Return T_sat and x (if needed), or wrap in a result object
                return T_sat;
            }
            if (h < h_l){
                x = 0f;
            }
            if (h > h_v){
                x = 1f;
            }
        } catch (RuntimeException ignored) {

        }
        Float finalX = x;
        Function<Float, Float> residual = (Float TGuess) -> {
            float computedH = get_h(finalX !=null ? finalX :get_x(h,TGuess,P),TGuess,P);
            return Math.abs(h - computedH);
            /*try {
                double[] roots = EOS.getZFactors(Tguess, P);
                if (roots.length == 1) {
                    // Single phase
                    double Z = roots[0];
                    double Vm = Z * PengRobinsonEOS.R * Tguess / P;
                    double hComputed = EOS.totalEnthalpy(Tguess, Vm) * M;//this is a molar enthalpy we need a by mass
                    return (float) Math.abs(hComputed - h);
                } else if (roots.length >= 2) {
                    Couple<Double> V = EOS.getSaturationVolumes(Tguess);

                    double hl = EOS.totalEnthalpy(Tguess, V.get(true)) * M;
                    double hv = EOS.totalEnthalpy(Tguess, V.get(false)) * M;
                    double xGuess = get_x(h, Tguess, P);
                    // If h within [hl, hv], calculate quality and mixture enthalpy
                    if (h >= hl && h <= hv) {
                        return 0f; // found T: h is between saturated enthalpies
                    }

                    // Otherwise, pick closest pure-phase approximation
                    double hClosest = (Math.abs(h - hl) < Math.abs(h - hv)) ? hl : hv;
                    System.out.println("weird choice + error : "+ Math.abs(hClosest - h)+ " at T = "+ Tguess+ " with hv "+ hv + " hl "+ hl);
                    return (float)Math.abs(hClosest - h);
                }
            } catch (Exception e) {
                return Float.MAX_VALUE;
            }
            return Float.MAX_VALUE;*/
        };

        float T = Solvers.gradientDecent(residual, 273 + h/2e6f, 0.01f, 0.01f, 0.001f);
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
        if (T < 100) T = 100;
        if (P < 1) P = 1;
        double[] roots = EOS.getZFactors(T, P);
        if (roots.length == 0) return 0;
        Couple<Double> Vms = EOS.getSaturationVolumes(T);
        if ( Math.abs(Vms.getSecond() - Vms.getFirst()) < 1e-8 ) {
            double V = roots[0] * R * T / P;
            return (float) ((V > Vms.getSecond()) ? 1.0 : 0.0);
        }
        try {
            double saturationPressure = EOS.saturationPressure(T);
            if (P < saturationPressure *0.99f) return  1f;
            if (P > saturationPressure * 1.01f) return   0f;
        } catch (RuntimeException e){

        }
        /*if (roots.length < 2) {
            // Not in coexistence region → determine if it's clearly vapor or liquid

            if (roots.length == 1) {
                ; // crude guess
            }

            FormicAPI.LOGGER.debug("Not in coexistence region and cannot infer phase, this shouldn't be  possible, check " +
                    "that the EOS is cubic : "+ Arrays.toString(roots)+ " T = "+ T + " P = "+ P);//should be impossible
            return 0; // defaulting to liquid
        }*/

        double hl = EOS.totalEnthalpy(T, Vms.getFirst());
        double hv = EOS.totalEnthalpy(T, Vms.getSecond());


        double denominator = hv - hl;
        if (Math.abs(denominator) < 1e-6) {
            // Avoid divide-by-zero
            return (float)((h > hl) ? 1.0 : 0.0);
        }

        float x = (float)((h - hl) / denominator);

        // Clamp to [0,1] to account for numerical error
        return Math.min(Math.max(x,0f), 1f);
    }
    /**
     * Estimate vapor quality `x` from entropy at given T and P.
     * Returns:
     *   - 0 for pure liquid
     *   - 1 for pure vapor
     *   - in (0, 1) for two-phase mixture
     */
    public static float get_x_from_entropy(double s, float T, float P) {//this is wrong.
        if (T < 100) T = 100;
        if (P < 1) P = 1;
        double[] roots = EOS.getZFactors(T, P);

        if (roots.length == 0){
            //System.out.println("out of validity limits with T = "+ T + " P = " + P);
            return 0;//fallback to 0 because we are at a specific volume lower than b
        }
        Couple<Double> Vms = EOS.getSaturationVolumes(T);
        if ( Math.abs(Vms.getSecond() - Vms.getFirst()) < 1e-8 ) {
            double V = roots[0] * R * T / P;
            return  (float) ((V > Vms.getSecond()) ? 1.0 : 0.0);
        }

        try {
            double saturationPressure = EOS.saturationPressure(T);
            if (P < saturationPressure *0.99f) return  1f;
            if (P > saturationPressure * 1.01f) return   0f;
        } catch (RuntimeException ignored){
        }


        double sl = EOS.totalEntropy(T, Vms.getFirst());   // saturated liquid
        double sv = EOS.totalEntropy(T, Vms.getSecond());  // saturated vapor

        final double epsilon = 1e-6;
        if (Math.abs(sl - sv) < epsilon) {
            // Avoid division by zero in pathological or near-critical cases
            return s < sl ? 0.0f : 1.0f;
        }

        float x = (float) ((s - sl) / (sv - sl));
        return Math.max(0.0f, Math.min(1.0f, x));
    }

    private static @NotNull SpecificRealGazState isentropicPressureChange(float T1, float P1, float x1, float finalPressure) {
        // Step 1: Compute constant entropy from initial state
        double sTarget = EOS.getEntropy(T1, P1, x1);

        // Step 2: Set parameters for stepping pressure
        float Tcurrent = T1;
        float xcurrent = x1;
        // Gradient descent to update T so that s(T, Pcurrent, x) ≈ sTarget
        float Tguess = Tcurrent;
        float learningRate = 0.05f;
        float tolerance = 1e-4f;
        int maxIterations = 1000;

        for (int iter = 0; iter < maxIterations; iter++) {
            xcurrent = get_x_from_entropy(sTarget, Tguess, finalPressure); // phase quality at this step
            double sGuess = EOS.getEntropy(Tguess, finalPressure, xcurrent);
            //this is xinda weird ...

            float error = (float) (sGuess - sTarget);
            //System.out.println("error "+ error);
            if (Math.abs(error) < tolerance) break;//like a gradient decent but the break condition is on the error not on it's derivative

            // Estimate entropy derivative w.r.t. T
            float deltaT = 1e-3f;
            //float xPlus = get_x_from_entropy(sGuess, Tguess + deltaT, finalPressure);
            double sPlus = EOS.getEntropy(Tguess + deltaT, finalPressure, xcurrent);
            float derivative = (float) ((sPlus - sGuess) / deltaT);

            if (Math.abs(derivative) < 1e-8f) derivative = 1e-8f;

            // Update guess
            Tguess -= learningRate * error / derivative;
            learningRate *= 0.99f;

            // Clamp
            if (Tguess < 1f) Tguess = 1f;
            if (Tguess > 5000f) Tguess = 5000f;
        }

        Tcurrent = Tguess;

        xcurrent = get_x_from_entropy(sTarget, Tcurrent, finalPressure);
        // Step 3: Compute final enthalpy
        float hFinal = get_h(xcurrent, Tcurrent, finalPressure);
        return new SpecificRealGazState(Tcurrent, finalPressure, hFinal, xcurrent);
    }
    public static @NotNull SpecificRealGazState isentropicPressureChange2(float T1, float P1, float x1, float finalPressure) {
        // Step 1: Compute constant entropy from initial state
        double sTarget = EOS.getEntropy(T1, P1, x1);

        // Step 2: Set parameters for stepping pressure
        float TCurrent = T1;
        float PCurrent = P1;
        float xcurrent = x1;
        // Gradient descent to update T so that s(T, Pcurrent, x) ≈ sTarget

        float dP = (finalPressure - P1)/10;
        //initial guess to make gradient descent go faster
        for (int iter = 0; iter < 10; iter++) {
            double baseS_P = EOS.getEntropy(TCurrent, PCurrent + dP, xcurrent);
            double baseS_T  = EOS.getEntropy(TCurrent + 1, PCurrent + dP, xcurrent);
            double dT = 1/(baseS_T - baseS_P);
            TCurrent += (float) dT;
            PCurrent += dP;
            double newS = EOS.getEntropy(TCurrent, PCurrent, xcurrent);
            xcurrent = get_x_from_entropy(newS, PCurrent, xcurrent);
        }
        //System.out.println("T first guess :"+TCurrent);
        Function<Float, Float> error = (T) -> {
            return (float) Math.abs(sTarget - EOS.getEntropy(T, finalPressure, get_x_from_entropy(sTarget, T, finalPressure)));
        };
        float TFinal = Solvers.gradientDecent(error, TCurrent, 0.1f, 0.01f, 1e-3f);
        //System.out.println("T final :"+TFinal);
        //xcurrent = get_x_from_entropy(sTarget, TCurrent, finalPressure);
        // Step 3: Compute final enthalpy
        float xFinal = get_x_from_entropy(sTarget, TFinal, finalPressure);
        float hFinal = get_h(xFinal, TFinal, finalPressure);
        return new SpecificRealGazState(TFinal, finalPressure, hFinal, xcurrent);
    }


    /**
     * adiabatic reversible expansion
     * @param initial
     * @param expansionFactor the initial pressure over the pressure of the fluid at the end of the turbine
     * @return the new fluid state
     */
    public static SpecificRealGazState isentropicExpansion(SpecificRealGazState initial, float expansionFactor) {

        return isentropicPressureChange(initial.temperature(), initial.pressure(), initial.vaporQuality(), initial.pressure() / expansionFactor);
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

        return isentropicPressureChange(initial.temperature(), initial.pressure(),initial.vaporQuality(), initial.pressure() *compressionFactor);

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
        System.out.println("first "+first+ " second"+second);
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