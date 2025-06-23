package com.rae.formicapi.math.data;

import java.util.Map;
import java.util.TreeMap;

public class OneDTabulatedFunction {
    private final TreeMap<Float, Float> table;
    private final float step;
    private final StepMode mode;
    private final boolean clamp;

    /**
     * @param table Sorted data: x -> y
     * @param step Distance between values in transformed space (e.g., log(x) if LOGARITHMIC)
     * @param mode The axis step mode (linear, log, etc.)
     * @param clamp Clamp outside instead of extrapolating.
     */
    public OneDTabulatedFunction(TreeMap<Float, Float> table, float step, StepMode mode, boolean clamp) {
        this.table = table;
        this.step = step;
        this.mode = mode;
        this.clamp = clamp;
    }

    private float evaluate(double input, TreeMap<Float, Float> table) {
        if (table.isEmpty()) {
            throw new IllegalStateException("Function table is empty");
        }

        float index = (float) (mode.forward.applyAsDouble(input) / step);
        int lowerIndex = (int) Math.floor(index);
        float frac = index - lowerIndex;

        float T1 = (float) mode.inverse.applyAsDouble(lowerIndex * step);
        float T2 = (float) mode.inverse.applyAsDouble((lowerIndex + 1) * step);

        // Safeguard in case floating-point precision causes a missing key
        if (!table.containsKey(T1) || !table.containsKey(T2)) {
            Map.Entry<Float, Float> lower = table.floorEntry((float) input);
            Map.Entry<Float, Float> upper = table.ceilingEntry((float) input);

            if (lower == null || upper == null) {
                return table.get(table.firstKey());
            }

            float T_lower = lower.getKey();
            float T_upper = upper.getKey();
            if (T_lower == T_upper) {
                return table.get(T_lower);
            }
            float fracAlt = (float) ((input - T_lower) / (T_upper - T_lower));

            return lower.getValue() * (1 - fracAlt) + upper.getValue() * fracAlt;
        }

        float P1 = table.get(T1);
        float P2 = table.get(T2);

        return P1 * (1 - frac) + P2 * frac;
    }

    public float evaluate(float output) {
        return evaluate(output, table);
    }

    private float extrapolateBelow(TreeMap<Float, Float> searchMap) {
        Map.Entry<Float, Float> lower = searchMap.firstEntry();
        Map.Entry<Float, Float> upper = searchMap.higherEntry(lower.getKey());
        if (upper == null) return lower.getValue();
        return linear(searchMap, lower, upper);
    }

    private float extrapolateAbove(TreeMap<Float, Float> searchMap) {
        Map.Entry<Float, Float> upper = searchMap.lastEntry();
        Map.Entry<Float, Float> lower = searchMap.lowerEntry(upper.getKey());
        if (lower == null) return upper.getValue();
        return linear(searchMap, lower, upper);
    }

    private float linear(TreeMap<Float, Float> map, Map.Entry<Float, Float> a, Map.Entry<Float, Float> b) {
        float x1 = a.getKey();
        float x2 = b.getKey();
        float y1 = a.getValue();
        float y2 = b.getValue();
        float query = map.firstKey(); // doesn't matter in this context
        float t = (query - x1) / (x2 - x1);
        return y1 * (1 - t) + y2 * t;
    }


}
