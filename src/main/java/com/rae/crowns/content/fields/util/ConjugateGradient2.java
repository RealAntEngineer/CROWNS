package com.rae.crowns.content.fields.util;

import com.rae.formicapi.fondation.math.operators.Matrix;
import com.rae.formicapi.fondation.math.solvers.LeastSquare2;

/**
 * Plain Conjugate Gradient solver for symmetric positive-definite systems {@code Ax = b}.
 *
 * <p>Prefer this over {@link LeastSquare2} (CGNE on {@code AᵀAx = Aᵀb}) when the matrix
 * is known to be SPD. CGNE squares the condition number, which causes slow or incorrect
 * convergence for weakly-diagonal-dominant rows (e.g. diffusion voxels with low resilience).
 * Plain CG works directly on {@code A} and converges in at most {@code n} iterations for
 * an exactly SPD system.
 *
 * <p>The physics matrix is SPD by construction:
 * <ul>
 *   <li>Diagonal: {@code 1 + res*β + Σcoeff > 0}</li>
 *   <li>Off-diagonals: {@code -coeff} symmetric (harmonic mean conductivity is the same
 *       in both directions)</li>
 *   <li>Diagonal dominance: {@code diag = 1 + res*β + Σcoeff > Σcoeff}</li>
 * </ul>
 */
public class ConjugateGradient2 {

    // -------------------------------------------------------------------------
    // Convenience overloads — allocate internally, for non-hot paths
    // -------------------------------------------------------------------------

    /**
     * Solve {@code Ax = b} with a zero initial guess, allocating working buffers internally.
     * Use only outside hot paths — prefer the pre-allocated overload at 20 ticks/s.
     */
    public static double[] solve(Matrix A, double[] b, int maxIter, double tol) {
        int n = A.rows();
        return solve(A, new double[n], b, maxIter, tol,
                new double[n], new double[n], new double[n]);
    }

    /**
     * Solve {@code Ax = b} from an initial guess, allocating working buffers internally.
     */
    public static double[] solve(Matrix A, double[] x_init, double[] b, int maxIter, double tol) {
        int n = A.rows();
        return solve(A, x_init, b, maxIter, tol,
                new double[n], new double[n], new double[n]);
    }

    // -------------------------------------------------------------------------
    // Primary overload — zero allocation, warm start, in-place
    // -------------------------------------------------------------------------

    /**
     * Solve {@code Ax = b} using Conjugate Gradient, with a warm start and
     * caller-supplied working buffers.
     *
     * <p>{@code x} is used as both the initial guess and the output — pass
     * {@code T_next} from the previous tick for warm-starting. The array is
     * overwritten in-place; no solution array is allocated.
     *
     * <p>All working arrays are overwritten on every call. Their values between
     * calls are undefined and must not be read by the caller.
     *
     * <p>Requires {@code A} to be square, symmetric, and positive definite.
     *
     * @param A       SPD matrix, must be square ({@code rows == cols})
     * @param x       initial guess on entry, solution on exit — length {@code n}
     * @param b       right-hand side — length {@code n}
     * @param maxIter maximum iterations before returning current best estimate
     * @param tol     convergence threshold on {@code ||r||₂}
     * @param r       pre-allocated residual buffer   — length ≥ {@code n}
     * @param p       pre-allocated direction buffer  — length ≥ {@code n}
     * @param Ap      pre-allocated {@code A·p} buffer — length ≥ {@code n}
     * @return {@code x} (same array as input, for chaining)
     * @throws IllegalArgumentException if matrix is not square or buffer sizes mismatch
     */
    public static double[] solve(Matrix A, double[] x, double[] b, int maxIter, double tol,
                                 double[] r, double[] p, double[] Ap) {
        int n = A.rows();
        int m = A.cols();

        if (n != m)
            throw new IllegalArgumentException(
                    "CG requires a square matrix: rows=" + n + ", cols=" + m);
        if (x.length != n)
            throw new IllegalArgumentException(
                    "x length (" + x.length + ") != matrix size (" + n + ")");
        if (b.length != n)
            throw new IllegalArgumentException(
                    "b length (" + b.length + ") != matrix size (" + n + ")");
        if (r.length < n || p.length < n || Ap.length < n)
            throw new IllegalArgumentException(
                    "Working buffers r/p/Ap must have length >= " + n);

        // r = b - A*x
        A.multiply(x, Ap); // use Ap as temp for the initial residual
        for (int i = 0; i < n; i++) {
            r[i] = b[i] - Ap[i];
            p[i] = r[i];
        }

        double rsold = dot(r, r, n);

        for (int k = 0; k < maxIter; k++) {
            A.multiply(p, Ap);

            double dotPAp = dot(p, Ap, n);
            if (dotPAp == 0) break; // already at solution or breakdown

            double alpha = rsold / dotPAp;

            for (int i = 0; i < n; i++) x[i] += alpha * p[i];
            for (int i = 0; i < n; i++) r[i] -= alpha * Ap[i];

            double rsnew = dot(r, r, n);
            if (Math.sqrt(rsnew) < tol) break;

            double beta = rsnew / rsold;
            for (int i = 0; i < n; i++) p[i] = r[i] + beta * p[i];
            rsold = rsnew;
        }

        return x;
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static double dot(double[] a, double[] b, int n) {
        double sum = 0;
        for (int i = 0; i < n; i++) sum += a[i] * b[i];
        return sum;
    }
}
