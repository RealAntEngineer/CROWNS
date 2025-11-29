package com.rae.crowns;

import com.rae.crowns.content.fields.advection.VelocityDataLayer;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.fields.util.SectionLooper;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class MassConservationSLTest {
    /*
    public static void main(String[] args) throws FileNotFoundException {
        PhysicsWorldData data = new PhysicsWorldData();

        // Create velocity layers
        VelocityDataLayer vx = new VelocityDataLayer();
        VelocityDataLayer vy = new VelocityDataLayer();
        VelocityDataLayer vz = new VelocityDataLayer();

        long sectionPos = 0L;
        data.putLayer(DataLayerType.VX, sectionPos, vx);
        data.putLayer(DataLayerType.VY, sectionPos, vy);
        data.putLayer(DataLayerType.VZ, sectionPos, vz);

        // Initialize a simple divergence-free field
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    vx.set(x, y, z, 0.1f);
                    vy.set(x, y, z, 0.1f);
                    vz.set(x, y, z, 0.1f);
                }
            }
        }

        SectionLooper.Context ctx = new SectionLooper.Context();
        ctx.section(0, 0, 0);

        VelocityDataLayer vxNext = new VelocityDataLayer();
        VelocityDataLayer vyNext = new VelocityDataLayer();
        VelocityDataLayer vzNext = new VelocityDataLayer();

        float dt = 1f; // pseudo time step

        for (int step = 0; step < 10; step++) {
            vx.set(3, 8, 8, 1f); // initial peak

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {

                        // Get current velocity at this voxel
                        float u = vx.get(x, y, z);
                        float v = vy.get(x, y, z);
                        float w = vz.get(x, y, z);

                        // Backtrace: departure point for semi-Lagrangian
                        float x_d = x - dt * u;
                        float y_d = y - dt * v;
                        float z_d = z - dt * w;

                        // Clamp to domain boundaries
                        x_d = Math.max(0f, Math.min(15f, x_d));
                        y_d = Math.max(0f, Math.min(15f, y_d));
                        z_d = Math.max(0f, Math.min(15f, z_d));

                        // Sample previous velocities using trilinear interpolation
                        float uNext = trilinearSample(vx, x_d, y_d, z_d);
                        float vNext = trilinearSample(vy, x_d, y_d, z_d);
                        float wNext = trilinearSample(vz, x_d, y_d, z_d);

                        vxNext.set(x, y, z, uNext);
                        vyNext.set(x, y, z, vNext);
                        vzNext.set(x, y, z, wNext);
                    }
                }
            }

            // Print 2D slice at z=8
            int zSlice = 8;
            String filename = String.format("src/test/output/vx_t%02d.csv", step);
            try (PrintWriter pw = new PrintWriter(filename)) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        pw.print(vx.get(x, y, zSlice));
                        if (x < 15) pw.print(",");
                    }
                    pw.println();
                }
            }
            System.out.println("Saved " + filename);

            // Swap buffers for next step
            vx.fromBytes(vxNext.toBytes());
            vy.fromBytes(vyNext.toBytes());
            vz.fromBytes(vzNext.toBytes());
        }
    }

    // --- Trilinear interpolation helper ---
    private static float trilinearSample(VelocityDataLayer layer, float x, float y, float z) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        int x1 = Math.min(x0 + 1, 15);
        int y1 = Math.min(y0 + 1, 15);
        int z1 = Math.min(z0 + 1, 15);

        float xd = x - x0;
        float yd = y - y0;
        float zd = z - z0;

        float c00 = layer.get(x0, y0, z0) * (1 - xd) + layer.get(x1, y0, z0) * xd;
        float c01 = layer.get(x0, y0, z1) * (1 - xd) + layer.get(x1, y0, z1) * xd;
        float c10 = layer.get(x0, y1, z0) * (1 - xd) + layer.get(x1, y1, z0) * xd;
        float c11 = layer.get(x0, y1, z1) * (1 - xd) + layer.get(x1, y1, z1) * xd;

        float c0 = c00 * (1 - yd) + c10 * yd;
        float c1 = c01 * (1 - yd) + c11 * yd;

        return c0 * (1 - zd) + c1 * zd;
    }*/
}

