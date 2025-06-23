package com.rae.formicapi.math.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class ReversibleOneDTabulatedFunction {

    private final OneDTabulatedFunction f;
    private final OneDTabulatedFunction inverse_f;

    public ReversibleOneDTabulatedFunction(Function<Float, Float>  f, float min, float max, StepMode stepMode, float step, StepMode inverseStepMode, float inverseStep) {
        TreeMap<Float, Float> populated = populate(f, min, max, stepMode, step);
        this.f = new OneDTabulatedFunction(populated, step, stepMode, true);
        inverse_f = new OneDTabulatedFunction(invertTable(populated, inverseStepMode, inverseStep), inverseStep, inverseStepMode, true);
    }

    private TreeMap<Float, Float> populate(Function<Float, Float>  f, float min, float max, StepMode stepMode, float step) {
        TreeMap<Float, Float> treeMap = new TreeMap<>();
        for (float x = min; x < max; x= (float) stepMode.inverse.applyAsDouble(stepMode.forward.applyAsDouble(x) + step)) {
            try {
                float P = f.apply(x);
                treeMap.put(x, P);
            } catch (Exception ignored) {
            }
        }
        return treeMap;
    }


    /**
     * Returns a reversed version of the map: y -> x
     */
    private TreeMap<Float, Float> invertTable(TreeMap<Float, Float> original, StepMode stepMode, float step) {
        TreeMap<Float, Float> inverted = new TreeMap<>();
        // Step 2: Build regular grid in log(P) space
        List<Map.Entry<Float, Float>> entries = new ArrayList<>(original.entrySet());

        // Determine the log pressure range
        float logP_start = (float) stepMode.forward.applyAsDouble(entries.get(0).getValue());
        float logP_end = (float) stepMode.forward.applyAsDouble(entries.get(entries.size() - 1).getValue());
        int numSteps = (int) ((logP_end - logP_start) / step);


        for (int i = 0; i < numSteps; i++) {
            float targetLogP = logP_start + i * step;

            // Find where targetLogP fits between logP1 and logP2
            for (int j = 0; j < entries.size() - 1; j++) {
                float T1 = entries.get(j).getKey();
                float T2 = entries.get(j + 1).getKey();
                float logP1 = (float) stepMode.forward.applyAsDouble(entries.get(j).getValue());
                float logP2 = (float) stepMode.forward.applyAsDouble(entries.get(j + 1).getValue());

                if (targetLogP >= logP1 && targetLogP <= logP2 && logP1 != logP2) {
                    // Linear interpolation in logP
                    float t = (targetLogP - logP1) / (logP2 - logP1);
                    float T_interp = T1 + t * (T2 - T1);

                    inverted.put((float) stepMode.inverse.applyAsDouble(targetLogP), T_interp);
                    break;
                }
            }
        }
        return inverted;
    }

    public float getF(float x) {
        return f.evaluate(x);
    }

    public float getInverseF(float x) {
        return inverse_f.evaluate(x);
    }
}
