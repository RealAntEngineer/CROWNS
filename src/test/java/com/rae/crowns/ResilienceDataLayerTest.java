package com.rae.crowns;


import com.rae.crowns.content.fields.temperature.ResilienceDataLayer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

public class ResilienceDataLayerTest {

    @Test
    void testEdgeValues() {
        ResilienceDataLayer layer = new ResilienceDataLayer();

        // Test minimum
        layer.set(0, 0, 0, 0f);
        assertEquals(0f, layer.get(0, 0, 0), 1e-6f);

        // Test maximum
        layer.set(1, 0, 0, 1f);
        assertEquals(1f, layer.get(1, 0, 0), 1e-6f);
    }

    @Test
    void testMiddleValue() {
        ResilienceDataLayer layer = new ResilienceDataLayer();

        // Middle value 0.5
        layer.set(0, 1, 0, 0.5f);
        float got = layer.get(0, 1, 0);
        // Should be approximately 0.5
        assertTrue(Math.abs(got - 0.5f) < 1f / 255f, "Got: " + got);
    }

    @Test
    void testRandomValues() {
        ResilienceDataLayer layer = new ResilienceDataLayer();
        Random rand = new Random(12345);

        for (int i = 0; i < 1000; i++) {
            float val = rand.nextFloat(); // 0..1
            layer.set(0, 0, 0, val);
            float got = layer.get(0, 0, 0);
            // Should be close to original, with max error 1/255
            assertTrue(Math.abs(got - val) <= 1f / 255f, "Val: " + val + ", Got: " + got);
        }
    }

    @Test
    void testSerialization() {
        ResilienceDataLayer layer = new ResilienceDataLayer();
        layer.set(3, 2, 1, 0.75f);

        byte[] bytes = layer.toBytes();
        ResilienceDataLayer loaded = new ResilienceDataLayer().fromBytes(bytes);
    }
}