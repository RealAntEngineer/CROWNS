package com.rae.crowns.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RayTraceUtil {

    public static @NotNull List<BlockPos> getSphereSurface(@NotNull BlockPos center, int radius, boolean empty) {
        List<BlockPos> blocks = new ArrayList<>();

        int bx = center.getX();
        int by = center.getY();
        int bz = center.getZ();

        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int y = by - radius; y <= by + radius; y++) {
                for (int z = bz - radius; z <= bz + radius; z++) {
                    double distance = ((bx - x) * (bx - x) + (bz - z) * (bz - z) + (by - y) * (by - y));
                    if (distance < radius * radius && (!empty || distance >= (radius - 1) * (radius - 1))) {
                        blocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return blocks;
    }


    public static @NotNull List<Vec3> getSphereSurface(@NotNull Vec3 center, float radius) {
        List<Vec3> blocks = new ArrayList<>();

        double bx = center.x();
        double by = center.y();
        double bz = center.z();

        int ceilR = (int) Math.ceil(radius);

        double r2      = radius * radius;
        double rInner2 = (radius - 1) * (radius - 1);

        for (int dy = -ceilR; dy <= ceilR; dy++) {
            double y  = by + dy;
            double y2 = (dy) * (dy);

            double crossR2 = r2 - y2;
            if (crossR2 < 0) continue; // outside sphere

            double crossR = Math.sqrt(crossR2);

            int crossCeil = (int) Math.ceil(crossR);
            for (int dx = -crossCeil; dx <= crossCeil; dx++) {
                double x   = bx + dx;
                double dx2 = dx * dx;

                for (int dz = -crossCeil; dz <= crossCeil; dz++) {
                    double z   = bz + dz;
                    double dz2 = dz * dz;

                    double dist2 = dx2 + dy * dy + dz2;
                    if (dist2 <= r2 && dist2 >= rInner2) {
                        blocks.add(new Vec3(x, y, z));
                    }
                }
            }
        }

        return blocks;
    }
}
