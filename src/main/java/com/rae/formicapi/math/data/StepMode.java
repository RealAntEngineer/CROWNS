package com.rae.formicapi.math.data;

import java.util.function.DoubleUnaryOperator;

public enum StepMode {
    LINEAR(
            x -> x,
            x -> x
    ),
    LOGARITHMIC(
            Math::log,
            Math::exp
    );

    public final DoubleUnaryOperator forward;
    public final DoubleUnaryOperator inverse;

    StepMode(DoubleUnaryOperator forward, DoubleUnaryOperator inverse) {
        this.forward = forward;
        this.inverse = inverse;
    }
}
