package com.rae.crowns.content.fields.util;

public class ConjugateGradientSolver {

    // Solve A * x = b
    // A is defined implicitly by matVec callback
    public static void solve(
            MatrixOperator A,
            float[] b,
            float[] x,
            int maxIter,
            float tol,
            float[] preconditioner // can be null
    ) {
        int n = b.length;
        float[] r = new float[n];
        float[] z = new float[n];
        float[] p = new float[n];
        float[] Ap = new float[n];

        // r = b - A*x
        A.multiply(x, r);
        for (int i = 0; i < n; i++) r[i] = b[i] - r[i];

        // z = M^-1 * r (if preconditioner present)
        if (preconditioner != null) {
            for (int i = 0; i < n; i++) z[i] = r[i] * preconditioner[i];
        } else {
            System.arraycopy(r, 0, z, 0, n);
        }

        System.arraycopy(z, 0, p, 0, n);
        float rzOld = dot(r, z);

        for (int iter = 0; iter < maxIter; iter++) {
            A.multiply(p, Ap);
            float alpha = rzOld / dot(p, Ap);

            // x = x + alpha * p
            // r = r - alpha * Ap
            for (int i = 0; i < n; i++) {
                x[i] += alpha * p[i];
                r[i] -= alpha * Ap[i];
            }

            float resNorm = (float) Math.sqrt(dot(r, r));
            if (resNorm < tol) {
                //System.out.println("CG converged in " + iter + " iterations, residual " + resNorm);
                break;
            }

            if (preconditioner != null) {
                for (int i = 0; i < n; i++) z[i] = r[i] * preconditioner[i];
            } else {
                System.arraycopy(r, 0, z, 0, n);
            }

            float rzNew = dot(r, z);
            float beta = rzNew / rzOld;
            for (int i = 0; i < n; i++) {
                p[i] = z[i] + beta * p[i];
            }

            rzOld = rzNew;
        }
    }

    private static float dot(float[] a, float[] b) {
        float s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    // Interface for matrix-vector product
    public interface MatrixOperator {
        void multiply(float[] x, float[] result);
    }
}

