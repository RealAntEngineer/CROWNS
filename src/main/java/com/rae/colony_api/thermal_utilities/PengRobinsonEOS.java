package com.rae.colony_api.thermal_utilities;


import com.rae.colony_api.math.Solvers;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PengRobinsonEOS implements EquationOfState {
    public static final double R = 8.314462618; // [J/mol·K]
    private final double Tc;  // Critical temperature [K]
    private final double Pc;  // Critical pressure [Pa]
    private final double omega; // Acentric factor
    private final double kappa;
    private final double a0;
    private final double b;
    //TODO add molar weight to transform molar
    public PengRobinsonEOS(double Tc, double Pc, double omega) {
        this.Tc = Tc;
        this.Pc = Pc;
        this.omega = omega;
        this.kappa = 0.37464 + 1.54226 * omega - 0.26992 * omega * omega;
        this.a0 = 0.45724 * R * R * Tc * Tc / Pc;
        this.b = 0.07780 * R * Tc / Pc;
    }

    public double alpha(double T) {
        double Tr = T / Tc;
        return Math.pow(1 + kappa * (1 - Math.sqrt(Tr)), 2);
    }

    public double a(double T) {
        return a0 * alpha(T);
    }

    @Override
    public double pressure(double T, double Vm) {
        double a = a(T);
        double term1 = R * T / (Vm - b);
        double term2 = a / (Vm * (Vm + b) + b * (Vm - b));
        return term1 - term2;
    }
    @Override
    public double volumeMolar(double T, double P, double vaporFraction) {
        double[] roots = getZFactors(T, P);

        // Filter for real positive roots
        double[] realRoots = Arrays.stream(roots)
                .filter(d -> !Double.isNaN(d) && d > 0)
                .sorted()
                .toArray();

        if (realRoots.length == 0) {
            throw new IllegalStateException("No valid real roots for Z.");
        }

        double Z;
        if (vaporFraction <= 0.0) {
            Z = realRoots[0]; // liquid root (smallest Z)
        } else if (vaporFraction >= 1.0) {
            Z = realRoots[realRoots.length - 1]; // vapor root (largest Z)
        } else if (realRoots.length >= 2) {
            double Zl = realRoots[0];
            double Zv = realRoots[realRoots.length - 1];
            Z = (1 - vaporFraction) * Zl + vaporFraction * Zv;
        } else {
            // Only one root, fallback
            Z = realRoots[0];
        }

        return Z * R * T / P;
    }

    public double @NotNull [] getZFactors(double T, double P) {
        double A = a(T) * P / (R * R * T * T);
        double B = b * P / (R * T);

        // Coefficients of the cubic in Z
        double c1 = 1.0;
        double c2 = -(1.0 - B);
        double c3 = A - 3 * B * B - 2 * B;
        double c4 = -(A * B - B * B - B * B * B);

        // Solve cubic
        return Solvers.solveCubic(c1, c2, c3, c4);
    }

    private double entropyDifference(double T1, double T2, double V1, double V2, double Cv) {
        return Cv * Math.log(T2 / T1) + R * Math.log((V2 - b) / (V1 - b));
    }
    public double totalEntropy(double T, double Vm) {
        double T0 = 298.15;  // Reference temperature
        double P0 = 1e5;     // Reference pressure

        // Get Z and Vm at reference state
        double[] zRoots = getZFactors(T0, P0);
        if (zRoots.length == 0) throw new RuntimeException("No Z root at reference state");

        double Z0 = Arrays.stream(zRoots).max().getAsDouble(); // assume vapor
        double Vm0 = Z0 * R * T0 / P0;

        // Use ideal gas Cv for water vapor if needed (you can refine this)
        double Cv = 3.5 * R; // for monoatomic ideal gas; adjust for water (≈ 1.5R to 2.0R at high T)

        return entropyDifference(T0, T, Vm0, Vm, Cv); // s0 = 0
    }


    /**
     *
     * @param T temperature
     * @return molar enthalpy
     */
    private double idealGasEnthalpy(double T) {
        double Cp = 75.0; // J/mol·K
        return Cp * T;
    }

    /**
     *
     * @param T temperature
     * @param Vm specific volume
     * @return residual Enthalpy
     */
    private double residualEnthalpy(double T, double Vm) {
        double a = a(T);
        double P = pressure(T, Vm);
        double Z = P * Vm / (R * T);

        double logTerm = Math.log((Vm + (1 + Math.sqrt(2)) * b) / (Vm + (1 - Math.sqrt(2)) * b));
        double hRes = T * (Z - 1) * R - a / (b * Math.sqrt(8)) * logTerm;

        return hRes;
    }

    public double totalEnthalpy(double T, double Vm) {
        return idealGasEnthalpy(T) + residualEnthalpy(T, Vm);
    }

    public double getB() {
        return b;
    }//this should be the starting point for plotting
}

