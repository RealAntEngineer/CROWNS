package com.rae.crowns;

import com.rae.crowns.content.fields.temperature.ConductionDataLayer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

public class ConductionDataLayerTest {

    @Test
    void testEdgeValues() {
        ConductionDataLayer layer = new ConductionDataLayer();

        // Test min value
        layer.set(0, 0, 0, ConductionDataLayer.MIN_VALUE);
        float gotMin = layer.get(0, 0, 0);
        assertEquals(ConductionDataLayer.MIN_VALUE, gotMin, 1e-7f);

        // Test max value
        layer.set(1, 0, 0, ConductionDataLayer.MAX_VALUE);
        float gotMax = layer.get(1, 0, 0);
        assertEquals(ConductionDataLayer.MAX_VALUE, gotMax, 1e-3f); // small error due to mantissa
    }

    @Test
    void testRandomValues() {
        ConductionDataLayer layer = new ConductionDataLayer();
        Random rand = new Random(123);

        for (int i = 0; i < 1000; i++) {
            float val = (float) (1e-4 + rand.nextDouble() * 1e10); // arbitrary range
            layer.set(0, 0, 0, val);
            float got = layer.get(0, 0, 0);

            // Value must be clamped
            float expected = Math.min(Math.max(val, ConductionDataLayer.MIN_VALUE), ConductionDataLayer.MAX_VALUE);
            assertTrue(Math.abs(got - expected)/expected <= 0.5f, "Value: " + val + ", got: " + got);
        }
    }

    @Test
    void testSerialization() {
        ConductionDataLayer layer = new ConductionDataLayer();
        layer.set(3, 2, 1, 12345f);
        byte[] bytes = layer.toBytes();

        ConductionDataLayer loaded = new ConductionDataLayer().fromBytes(bytes);
        float got = loaded.get(3, 2, 1);
        assertEquals(layer.get(3, 2, 1), got, 1e-3f);
    }
}
