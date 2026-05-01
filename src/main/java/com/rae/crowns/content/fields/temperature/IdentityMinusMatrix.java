package com.rae.crowns.content.fields.temperature;

import com.rae.formicapi.fondation.math.operators.MutableMatrix;

public class IdentityMinusMatrix implements MutableMatrix {
    private final MutableMatrix A;

    public IdentityMinusMatrix(MutableMatrix A) {
        this.A = A;
    }

    @Override
    public int rows() {
        return A.rows();
    }

    @Override
    public int cols() {
        return A.cols();
    }

    @Override
    public double get(int i, int i1) {
        return 0;
    }

    @Override
    public void multiply(double[] x, double[] result) {
        // result = A * x
        A.multiply(x, result);

        // result = x - result  => (I - A)x
        for (int i = 0; i < result.length; i++) {
            result[i] = x[i] - result[i];
        }
    }

    @Override
    public void add(int i, int j, double v) {
        this.A.add(i, j, v);
    }

    @Override
    public void set(int i, int j, double v) {
        this.A.set(i, j, v);

    }
}
