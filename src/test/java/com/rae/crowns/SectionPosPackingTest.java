package com.rae.crowns;

import com.rae.crowns.content.fields.temperature.TemperatureTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SectionPosPackingTest {

    private static final Random RAND = new Random(1234);

    @Test
    void testSectionPosPackingEquivalence() {
        for (int i = 0; i < 10_000; i++) {
            int sx = RAND.nextInt(1 << 20) - (1 << 19);
            int sy = RAND.nextInt(256);
            int sz = RAND.nextInt(1 << 20) - (1 << 19);

            // Mojang reference
            long mojangPacked = SectionPos.asLong(sx, sy, sz);

            // Custom pack/unpack
            long customPacked = TemperatureTicker.packSection(sx, sy, sz);

            // Must match exactly
            assertEquals(mojangPacked, customPacked,
                    () -> String.format("Mismatch at (%d,%d,%d)", sx, sy, sz));

            // Unpack and compare values
            assertEquals(sx, TemperatureTicker.unpackSectionX(customPacked), "X mismatch");
            assertEquals(sy, TemperatureTicker.unpackSectionY(customPacked), "Y mismatch");
            assertEquals(sz, TemperatureTicker.unpackSectionZ(customPacked), "Z mismatch");
        }
    }

    @Test
    void testBlockPosSectionConversionConsistency() {
        for (int i = 0; i < 10_000; i++) {
            BlockPos pos = new BlockPos(
                    RAND.nextInt(10000) - 5000,
                    RAND.nextInt(256),
                    RAND.nextInt(10000) - 5000
            );


            long packed = TemperatureTicker.packSection(pos.getX(), pos.getY(), pos.getZ());

            // Ensure unpacking gives the same section coordinates
            assertEquals(pos.getX(), TemperatureTicker.unpackSectionX(packed));
            assertEquals(pos.getY(), TemperatureTicker.unpackSectionY(packed));
            assertEquals(pos.getZ(), TemperatureTicker.unpackSectionZ(packed));
        }
    }
}

