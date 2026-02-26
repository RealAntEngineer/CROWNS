package com.rae.crowns.content.fields.util;

import org.jetbrains.annotations.NotNull;

public class DiffusionMatrix implements ConjugateGradientSolver.MatrixOperator {
    private final float[] diag;
    private final int[][] neighbors;
    private final float[][] coeffs;
    private final int size;

    public DiffusionMatrix(float @NotNull [] diag, int[][] neighbors, float[][] coeffs) {
        this.diag = diag;
        this.neighbors = neighbors;
        this.coeffs = coeffs;
        this.size = diag.length;
    }

    @Override
    public void multiply(float[] x, float[] result) {
        for (int i = 0; i < size; i++) {
            float sum = diag[i] * x[i];
            for (int n = 0; n < 6; n++) {
                int j = neighbors[i][n];
                if (j >= 0)
                    sum += coeffs[i][n] * x[j];
            }
            result[i] = sum;
        }
    }
}
