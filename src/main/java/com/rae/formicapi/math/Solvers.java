package com.rae.formicapi.math;

import com.rae.crowns.CROWNS;

import java.util.function.Function;

import static java.lang.Math.abs;

public class Solvers {
    /**
     *
     * @param function the equation you want to solve
     * @param a first boundaries
     * @param b second boundaries
     * @param epsilon tolerance
     * @return the Solution if there is one or 0.
     */
    public static float dichotomy(Function<Float, Float> function, float a, float b, float epsilon) {
        try {
            if (function.apply(a) * function.apply(b) > 0) {  //Verification of the boundaries
                throw new RuntimeException("Wrong boundaries in dichotomy solver : a=" + a + "f(a)=" + function.apply(a) + "| b=" + b + " f(b)=" + function.apply(b));
            } else {
                float m = (float) ((a + b) / 2.);
                while (abs(a - b) > epsilon) {
                    if (function.apply(m) == 0.0) {
                        return m;
                    } else if (function.apply(a) * function.apply(m) > 0) {
                        a = m;
                    } else {
                        b = m;
                    }
                    m = (a + b) / 2;
                }
                return m;
            }
        } catch (RuntimeException e) {
            CROWNS.LOGGER.error(e.toString());
            return 0;
        }
    }
    //TODO complete a naive approach first

    /**
     * Uses gradient descent to find the minimum of a given function, with adaptive step size.
     * (generated in part with chatgpt)
     * @param function a function that has a minimum
     * @param start starting point
     * @param step initial learning rate
     * @param dx small delta used to estimate the derivative
     * @return the estimated x value at which the function has a minimum, return NaN if there is no solution found
     */
    public static float gradientDecent(Function<Float, Float> function, float start, float step, float dx) {
        float x = start;
        float learningRate = step;
        int maxIterations = 10000;
        float tolerance = 1e-6f;
        float decay = 0.9f; // How fast the step shrinks when progress slows
        float minStep = 1e-6f;

        for (int i = 0; i < maxIterations; i++) {
            float derivative = (function.apply(x + dx) - function.apply(x - dx)) / (2 * dx);
            float newX = x - learningRate * derivative;

            float currentValue = function.apply(x);
            float newValue = function.apply(newX);

            if (newValue < currentValue) {
                // Improvement, keep going
                x = newX;
                // Optionally increase the step a little
                learningRate *= 1.05f;
            } else {
                // No improvement, reduce step size
                learningRate *= decay;
                if (learningRate < minStep) {
                    return x;
                }
            }

            if (Math.abs(derivative) < tolerance) {
                return x;
            }
        }

        return Float.NaN;
    }

    /**
     * give the real solutions of the equation : a * x^3 + b * x^2 + c * x + d
     */
    public static double[] solveCubic(double a, double b, double c, double d) {
        // Normalize coefficients
        b /= a;
        c /= a;
        d /= a;

        double q = (3.0 * c - b * b) / 9.0;
        double r = (9.0 * b * c - 27.0 * d - 2.0 * b * b * b) / 54.0;
        double discriminant = q * q * q + r * r;

        double[] roots = new double[3];

        if (discriminant > 0) {
            // One real root
            double s = Math.cbrt(r + Math.sqrt(discriminant));
            double t = Math.cbrt(r - Math.sqrt(discriminant));
            roots[0] = -b / 3.0 + (s + t);
            roots[1] = Double.NaN;
            roots[2] = Double.NaN;
        } else {
            // Three real roots
            double theta = Math.acos(r / Math.sqrt(-q * q * q));
            double sqrtQ = Math.sqrt(-q);
            roots[0] = 2 * sqrtQ * Math.cos(theta / 3.0) - b / 3.0;
            roots[1] = 2 * sqrtQ * Math.cos((theta + 2 * Math.PI) / 3.0) - b / 3.0;
            roots[2] = 2 * sqrtQ * Math.cos((theta + 4 * Math.PI) / 3.0) - b / 3.0;
        }

        return roots;
    }
}
