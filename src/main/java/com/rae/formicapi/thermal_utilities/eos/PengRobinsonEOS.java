package com.rae.formicapi.thermal_utilities.eos;


import com.rae.formicapi.math.Solvers;
import com.rae.formicapi.math.data.ReversibleOneDTabulatedFunction;
import com.rae.formicapi.math.data.StepMode;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class PengRobinsonEOS extends CubicEOS{

    private final double kappa;
    private final double a0;
    private final double b;
    private final ReversibleOneDTabulatedFunction saturation;
    //TODO convert everything to float
    public PengRobinsonEOS(double Tc, double Pc, double omega, double M) {
        super(M, Tc,Pc);
        // Acentric factor
        this.kappa = 0.37464 + 1.54226 * omega - 0.26992 * omega * omega;
        this.a0 = 0.45724 * EquationOfState.R * EquationOfState.R * Tc * Tc / Pc;
        this.b = 0.07780 * EquationOfState.R * Tc / Pc;

        this.saturation = new ReversibleOneDTabulatedFunction((T) -> (float) computeSaturationPressure(T), 100F,
                (float)Tc, StepMode.LINEAR, (float) (Tc/1e4f), StepMode.LOGARITHMIC, 0.01f);
    }

    public double alpha(double T) {
        double Tr = T / Tc;
        return Math.pow(1 + kappa * (1 - Math.sqrt(Tr)), 2);
    }

    private double dAlpha_dT(double T) {
        double sqrtTr = Math.sqrt(T / Tc);
        return -kappa * (1 + kappa * (1 - sqrtTr)) / (Tc * sqrtTr);
    }

    private double da_dT(double T) {
        return a0 * dAlpha_dT(T); // ac is the "a" at critical temperature
    }

    public double a(double T) {
        return a0 * alpha(T);
    }

    @Override
    public double pressure(double T, double Vm) {
        double a = a(T);
        double term1 = EquationOfState.R * T / (Vm - b);
        double term2 = a / (Vm * (Vm + b) + b * (Vm - b));
        return term1 - term2;
    }

    @Override
    public List<Double> findSpinodalPoints(double T) {
        if (T >= Tc) return List.of();

        return findSpinodalPoints(T, b * 1.001f, 1e6);
    }

    /**
     *  What does this represent exactly ?
     * @param T the temperature in Kelvin, needs to be strictly positive
     * @param P the pressure in Pascals, needs to be strictly positive
     * @return the possible compressibility coefficient for the given isotherm and Pressure : minimal is the value at the saturation curve for liquid
     * the middle is non-physical, and the max is for the vapor
     */
    public double @NotNull [] getZFactors(double T, double P) {
        assert T > 0;
        assert P > 0;
        double A = a(T) * P / (EquationOfState.R * EquationOfState.R * T * T);
        double B = b * P / (EquationOfState.R * T);

        // Coefficients of the cubic in Z
        double c1 = 1.0;
        double c2 = -(1.0 - B);
        double c3 = A - 3 * B * B - 2 * B;
        double c4 = -(A * B - B * B - B * B * B);

        // Solve cubic
        return Arrays.stream(Solvers.solveCubic(c1, c2, c3, c4))
                .filter(d -> !Double.isNaN(d) && d > minZ((float) T, (float) P))
                .sorted()
                .toArray();
    }
    //not robust at low pressure
    public double fugacityCoefficient(double T, double P, double Z) {
        double R = EquationOfState.R;
        double a = a(T); // temperature-dependent 'a'
        double A = a * P / (R * R * T * T);
        double B = b * P / (R * T);
        double epsilon = 1e-9;
        if (B < epsilon) return 1.0; // Fugacity coefficient approaches 1 for ideal gas
        if (Math.abs(Z - B) < epsilon) return Double.NaN;

        double sqrt2 = Math.sqrt(2);

        double term1 = Z - 1 - Math.log(Z - B);
        double term2 = A / (2 * sqrt2 * B);
        double logArgument = (Z + (1 + sqrt2) * B) / (Z + (1 - sqrt2) * B);
        double term3 = term2 * Math.log(logArgument);

        double lnPhi = term1 - term3;

        return Math.exp(lnPhi);
    }
    @Override
    public double saturationPressure(double T) {
        if (T >= Tc) {
            throw new IllegalArgumentException("T must be lower than the critical temperature to compute a coexistence pressure");
        }
        if (T <= 0) {
            throw new IllegalArgumentException("Temperature must be positive");
        }

        return saturation.getF((float) T);
    }
    private double computeSaturationPressure(double T) {
        if (T >= Tc) {
            throw new IllegalArgumentException("T must be lower than the critical temperature to compute a coexistence pressure");
        }
        if (T <= 0) {
            throw new IllegalArgumentException("Temperature must be positive");
        }

        // Estimate pressure range based on spinodal points
        List<Double> spinodals = findSpinodalPoints(T);
        if (spinodals.size() < 2) {
            throw new RuntimeException("Not enough spinodal points found");
        }
        assert spinodals.size() == 2;

        double pMin = Math.max(1e-8, pressure(T, spinodals.get(0)));  // lower pressure bound
        double pMax = pressure(T, spinodals.get(1));               // upper pressure bound

        // Fugacity difference function
        Function<Float, Float> areaDifferenceFunction = (Float PGuess) -> {
            double[] roots = getZFactors(T, PGuess);
            if (roots.length < 2) return Float.MAX_VALUE;

            double Zl = Arrays.stream(roots).min().getAsDouble();
            double Zv = Arrays.stream(roots).max().getAsDouble();
            double Vl = Zl * EquationOfState.R * T / PGuess;
            double Vv = Zv * EquationOfState.R * T / PGuess;
            if (Math.abs(Vv - Vl) < 1e-9) return Float.MAX_VALUE;

            double area = 0;
            for (double V = Vl; V < Vv; V *= 1.01f) {
                double V2 = V*1.01f;
                double p1 = pressure(T, V);
                double p2 = pressure(T, V2);
                area += 0.5 * (p1 + p2) * (V2-V);
            }

            double rect = PGuess * (Vv - Vl);
            return (float)(area - rect);
        };
        try {
            return Math.max(1e-8,Solvers.dichotomy(areaDifferenceFunction, (float) pMin, (float) pMax, 200f));

        } catch (RuntimeException e) {
            return Math.max(1e-8,Solvers.gradientDecent((p )-> Math.abs(areaDifferenceFunction.apply(p)), (float) (pMin + pMax)/2, 100, 1, 2));

        }
    }


    public float saturationTemperature(float P) {
        if (P >= Pc) {
            throw new IllegalArgumentException("P must be lower than the critical pressure to compute a coexistence pressure");
        }
        if (P <= 0) {
            throw new IllegalArgumentException("Pressure must be positive");
        }
        return saturation.getInverseF(P);
    }
    @Override
    protected  double residualEntropy(double T, double P, double Z) {
        double a = a(T);
        double sqrt2 = Math.sqrt(2);
        double B = b * P / (R * T);

        double term1 = R * Math.log(Z - B);
        double term2 = (a / (2 * sqrt2 * b * R * T)) *
                Math.log((Z + (1 + sqrt2) * B) / (Z + (1 - sqrt2) * B));

        return (term1 - term2)/M; // [J/Kg·K]
    }

    @Override
    public double residualEnthalpy(double T, double P, double Z) {
        double a = a(T);
        double da_dT = da_dT(T); // must be implemented
        double B = b * P / (R * T);
        double sqrt2 = Math.sqrt(2);

        double logTerm = Math.log((Z + (1 + sqrt2) * B) / (Z + (1 - sqrt2) * B));
        double term1 = R * T * (Z - 1);
        double term2 = (T * da_dT - a) / (2 * sqrt2 * b) * logTerm;

        return (term1 + term2)/M; // [J/Kg]
    }

    public double getB() {
        return b;
    }//this should be the starting point for plotting
    private double minZ(float T, float P) {
        return (b *1.001f) * P / (EquationOfState.R * T); // Minimum physically valid Z
    }
    @Override
    public double volumeMolar(double T, double P, double vaporFraction) {
        try {
            return super.volumeMolar(T, P, vaporFraction);
        } catch (RuntimeException e) {
            return getB() * 1.001f;
        }
    }
}

