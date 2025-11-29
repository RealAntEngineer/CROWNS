package com.rae.crowns;

import com.rae.crowns.content.fields.advection.VelocityDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.NeighborVisitors;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.fields.util.SectionLooper;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class MassConservationTest {
    /*
    public static void main(String[] args) throws FileNotFoundException {
        PhysicsWorldData data = new PhysicsWorldData();

        // Create velocity layers
        TemperatureDataLayer vx = new TemperatureDataLayer();//better precision with this layer
        TemperatureDataLayer vy = new TemperatureDataLayer();
        TemperatureDataLayer vz = new TemperatureDataLayer();

        long sectionPos = 0L;

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

        // Derivative visitors
        NeighborVisitors.ScalarDerivativeNeighborVisitor vxVisitor =
                new NeighborVisitors.ScalarDerivativeNeighborVisitor(data, DataLayerType.VX);
        NeighborVisitors.ScalarDerivativeNeighborVisitor vyVisitor =
                new NeighborVisitors.ScalarDerivativeNeighborVisitor(data, DataLayerType.VY);
        NeighborVisitors.ScalarDerivativeNeighborVisitor vzVisitor =
                new NeighborVisitors.ScalarDerivativeNeighborVisitor(data, DataLayerType.VZ);

        SectionLooper.Context ctx = new SectionLooper.Context();
        ctx.section(0, 0, 0);

        // Create output velocity fields
        TemperatureDataLayer vxNext = new TemperatureDataLayer();
        TemperatureDataLayer vyNext = new TemperatureDataLayer();
        TemperatureDataLayer vzNext = new TemperatureDataLayer();

        float dt = 0.01f; // pseudo time step
        for (int i = 0; i < 1000; i++) {
            vx.set(0, 8, 8, 10f);

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        ctx.pos(x, y, z);

                        vxVisitor.setContext(sectionPos, x, y, z, vx.get(x, y, z));
                        vyVisitor.setContext(sectionPos, x, y, z, vy.get(x, y, z));
                        vzVisitor.setContext(sectionPos, x, y, z, vz.get(x, y, z));

                        ctx.forEachNeighbor(vxVisitor);
                        ctx.forEachNeighbor(vyVisitor);
                        ctx.forEachNeighbor(vzVisitor);

                        float[] dx = vxVisitor.getResult();
                        float[] dy = vyVisitor.getResult();
                        float[] dz = vzVisitor.getResult();

                        // Current velocities
                        float u = vx.get(x, y, z);
                        float v = vy.get(x, y, z);
                        float w = vz.get(x, y, z);

                        // --- Linear convection (explicit upwind scheme) ---
                        // du/dt = - (u * du/dx + v * du/dy + w * du/dz)
                        float du_dt = -(u * dx[0] + v * dx[1] + w * dx[2]);
                        float dv_dt = -(u * dy[0] + v * dy[1] + w * dy[2]);
                        float dw_dt = -(u * dz[0] + v * dz[1] + w * dz[2]);

                        // --- Viscosity (diffusion term) ---
                        // Laplacian: ∇²u = d²u/dx² + d²u/dy² + d²u/dz²
                        float nu = 0.01f; // viscosity coefficient (adjust for stability)

                        float lapU = dx[3] + dx[4] + dx[5];
                        float lapV = dy[3] + dy[4] + dy[5];
                        float lapW = dz[3] + dz[4] + dz[5];

                        du_dt += nu * lapU;
                        dv_dt += nu * lapV;
                        dw_dt += nu * lapW;

                        // --- Forward Euler update ---
                        float uNext = u + dt * du_dt;
                        float vNext = v + dt * dv_dt;
                        float wNext = w + dt * dw_dt;

                        vxNext.set(x, y, z, uNext);
                        vyNext.set(x, y, z, vNext);
                        vzNext.set(x, y, z, wNext);
                    }
                }
            }


            // --- Save current VX slice to file ---
            if (i % 10 == 0) {
                int zSlice = 8;
                String filename = String.format("src/test/output/vx_t%02d.csv", i);
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
            }

            vx.fromBytes(vxNext.toBytes());
            vy.fromBytes(vyNext.toBytes());
            vz.fromBytes(vzNext.toBytes());
        }
    }*/
}


