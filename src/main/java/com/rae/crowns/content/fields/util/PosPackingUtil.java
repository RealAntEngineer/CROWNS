package com.rae.crowns.content.fields.util;

import net.minecraft.util.Mth;

public class PosPackingUtil {

    // These are constants from Minecraft’s internal layout
    final static int PACKED_X_LENGTH = 1 + Mth.log2(Mth.smallestEncompassingPowerOfTwo(30000000));
    final static int PACKED_Z_LENGTH = PACKED_X_LENGTH;
    final static int PACKED_Y_LENGTH = 64 - PACKED_X_LENGTH - PACKED_Z_LENGTH;
    final static long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    final static int Z_OFFSET = PACKED_Y_LENGTH;
    final static int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;
    final static long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
    final static long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
    final static int Y_OFFSET = 0;

    // --- packing/unpacking kept identical to your original scheme ---
    public static long packSection(int sx, int sy, int sz) {
        return ((long) sx & 0x3FFFFF) << 42
                | ((long) sz & 0x3FFFFF) << 20
                | ((long) sy & 0xFFFFF);
    }

    public static int unpackSectionX(long packed) {
        return (int) (packed >> 42);
    }

    public static int unpackSectionY(long packed) {
        return (int) (packed << 44 >> 44);
    }

    public static int unpackSectionZ(long packed) {
        return (int) (packed << 22 >> 42);
    }


    /**
     * Packs absolute block coordinates (x, y, z) into a long,
     * using the same bit layout as Minecraft's BlockPos.asLong().
     * This avoids creating a BlockPos object.
     */
    public static long packBlockPos(int x, int y, int z) {
        // Perform the same bit-packing as BlockPos.asLong()
        long result = 0L;
        result |= ((long) x & PACKED_X_MASK) << X_OFFSET;
        result |= ((long) y & PACKED_Y_MASK) << Y_OFFSET;
        result |= ((long) z & PACKED_Z_MASK) << Z_OFFSET;
        return result;
    }
}
