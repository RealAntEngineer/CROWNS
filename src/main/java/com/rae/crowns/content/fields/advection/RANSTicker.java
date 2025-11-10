package com.rae.crowns.content.fields.advection;

import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.fields.util.SectionLooper;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * CFD / RANS-style ticker that updates VX/VY/VZ using a simple viscous diffusion + damping model.
 *
 * Expected layers passed to SectionLooper.iterate (per section):
 *   0 -> VX (float layer)
 *   1 -> BLOCKED_X (blocked mask, 1f = blocked)
 *   2 -> VY
 *   3 -> BLOCKED_Y
 *   4 -> VZ
 *   5 -> BLOCKED_Z
 */
public class RANSTicker {

    public static int TICK_PERIOD = 1;
    public static float DT = TICK_PERIOD / 20f;

    // Viscosity coefficient (nu). Tune to taste.
    public static final float VISCOSITY = 0.02f;

    // Linear damping (viscous dissipation / numerical stability)
    public static final float DISSIPATION = 0.01f;

    public static void tick(@NotNull Set<Long> tickingSections, @NotNull PhysicsWorldData data) {

        // --- MAIN TICK LOOP ---
        VelocityVoxelVisitor visitor = new VelocityVoxelVisitor(data);
        SectionLooper.iterate(tickingSections,(sectionPos) ->
                        data.getLayers(
                                sectionPos,
                                DataLayerType.VX,
                                DataLayerType.BLOCKED_X,
                                DataLayerType.VY,
                                DataLayerType.BLOCKED_Y,
                                DataLayerType.VZ,
                                DataLayerType.BLOCKED_Z
                                )
                , visitor);
    }

    /**
     * Visitor that computes one explicit viscous-diffusion + damping step for vx/vy/vz.
     * Uses blocked-face masks to prevent flux across solid faces.
     */
    private static final class VelocityVoxelVisitor implements SectionLooper.VoxelVisitor {
        private final PhysicsWorldData data;
        private final VelocityNeighborVisitor neighborVisitor;

        public VelocityVoxelVisitor(PhysicsWorldData data) {
            this.data = data;
            this.neighborVisitor = new VelocityNeighborVisitor(data);
        }

        @Override
        public void visit(long packedSection, int sx, int sy, int sz, int x, int y, int z, SectionLooper.Context ctx) {
            // read inputs from ctx in the order defined in iterate(...)
            float vx = ctx.getData(0);
            boolean blockedX = ctx.getData(1) >= 0.5f;
            float vy = ctx.getData(2);
            boolean blockedY = ctx.getData(3) >= 0.5f;
            float vz = ctx.getData(4);
            boolean blockedZ = ctx.getData(5) >= 0.5f;

            // set up neighbor visitor with all shared context needed
            neighborVisitor.setContext(packedSection, x, y, z, vx, vy, vz, blockedX, blockedY, blockedZ);

            // iterate neighbors (neighborVisitor will accumulate laplacian contributions for open faces)
            ctx.forEachNeighbor(neighborVisitor);

            // compute laplacian-scaled increments
            if (neighborVisitor.neighborCount == 0) {
                // no fluid neighbors (isolated), enforce zero velocity if fully blocked
                if (blockedX && blockedY && blockedZ) {
                    if (vx != 0f || vy != 0f || vz != 0f) {
                        ctx.setData(0, 0f);
                        ctx.setData(2, 0f);
                        ctx.setData(4, 0f);
                        data.setDirty(packedSection);
                    }
                }
                return;
            }

            float lapScale = 1f / neighborVisitor.neighborCount;

            float dvx = VISCOSITY * neighborVisitor.lapX * lapScale;
            float dvy = VISCOSITY * neighborVisitor.lapY * lapScale;
            float dvz = VISCOSITY * neighborVisitor.lapZ * lapScale;

            float newVx = vx + dvx * DT - DISSIPATION * vx * DT;
            float newVy = vy + dvy * DT - DISSIPATION * vy * DT;
            float newVz = vz + dvz * DT - DISSIPATION * vz * DT;

            if (vx > 0){
                System.out.println(newVx + " " + newVy + " " + newVz);
            }

            // sanitize
            if (Float.isNaN(newVx) || Float.isInfinite(newVx)) newVx = 0f;
            if (Float.isNaN(newVy) || Float.isInfinite(newVy)) newVy = 0f;
            if (Float.isNaN(newVz) || Float.isInfinite(newVz)) newVz = 0f;

            // write back (note ctx indices for vx/vy/vz are 0..2)
            ctx.setData(0, newVx);
            ctx.setData(2, newVy);
            ctx.setData(4, newVz);

            // mark dirty when changed meaningfully and propagate across sections if needed
            if (Math.abs(newVx - vx) > 1e-6f || Math.abs(newVy - vy) > 1e-6f || Math.abs(newVz - vz) > 1e-6f) {
                data.setDirty(packedSection);
                ctx.forEachNeighbor((nx, ny, nz, ref) -> {
                    if (ref.packedSection() != packedSection) data.setDirty(ref.packedSection());
                });
            } else {
                data.setClean(packedSection);
            }
        }
    }

    /**
     * Neighbor consumer that computes laplacian contributions for velocity,
     * skipping faces that are blocked by the per-face masks.
     */
    private static final class VelocityNeighborVisitor implements SectionLooper.Context.NeighborConsumer {
        private final PhysicsWorldData data;

        // context (set before each visit)
        long packedSection;
        int x, y, z;
        float selfVx, selfVy, selfVz;
        boolean blockedX, blockedY, blockedZ;

        // accumulated laplacian
        float lapX, lapY, lapZ;
        int neighborCount;

        VelocityNeighborVisitor(PhysicsWorldData data) {
            this.data = data;
        }

        void setContext(long packedSection, int x, int y, int z,
                        float vx, float vy, float vz,
                        boolean blockedX, boolean blockedY, boolean blockedZ) {
            this.packedSection = packedSection;
            this.x = x;
            this.y = y;
            this.z = z;
            this.selfVx = vx;
            this.selfVy = vy;
            this.selfVz = vz;
            this.blockedX = blockedX;
            this.blockedY = blockedY;
            this.blockedZ = blockedZ;
            this.lapX = 0f;
            this.lapY = 0f;
            this.lapZ = 0f;
            this.neighborCount = 0;
        }

        @Override
        public void accept(int nx, int ny, int nz, SectionLooper.NeighborRef ref) {
            // compute offset/direction from current to neighbor (should be unit in each component)
            int dx = nx - x;
            int dy = ny - y;
            int dz = nz - z;

            // --- NO-SLIP WALL CHECK --- (ignore it for now -> it will mean no redirection of the flow but some propagation)
            /*if (isFaceBlocked(dx, dy, dz)) {
                // Apply no-slip boundary condition: wall has velocity 0
                lapX += (0f - selfVx);
                lapY += (0f - selfVy);
                lapZ += (0f - selfVz);
                neighborCount++;
                return;
            }*/

            // fetch neighbor conduction/velocity layers from world data (neighbor section)
            VelocityDataLayer nVxLayer = data.getLayer(DataLayerType.VX, ref.packedSection());
            VelocityDataLayer nVyLayer = data.getLayer(DataLayerType.VY, ref.packedSection());
            VelocityDataLayer nVzLayer = data.getLayer(DataLayerType.VZ, ref.packedSection());

            if (nVxLayer == null || nVyLayer == null || nVzLayer == null) return;

            float nvx = nVxLayer.get(ref.localX(), ref.localY(), ref.localZ());
            float nvy = nVyLayer.get(ref.localX(), ref.localY(), ref.localZ());
            float nvz = nVzLayer.get(ref.localX(), ref.localY(), ref.localZ());

            // accumulate discrete Laplacian contribution (neighbor - self) for each component
            lapX += (nvx - selfVx);
            lapY += (nvy - selfVy);
            lapZ += (nvz - selfVz);

            neighborCount++;
        }

        private boolean isFaceBlocked(int dx, int dy, int dz) {
            // dx == +1 : face between (x,y,z) and (x+1,y,z) -> check BLOCKED_X at (x,y,z)
            // dx == -1 : face between (x-1,y,z) and (x,y,z) -> check BLOCKED_X at (x-1,y,z)
            // similarly for dy/dz with BLOCKED_Y / BLOCKED_Z.

            if (dx == 1) return blockedX;
            if (dx == -1) {
                return blockedX;
            }

            if (dy == 1) return blockedY;
            if (dy == -1) return blockedY;

            if (dz == 1) return blockedZ;
            if (dz == -1) return blockedZ;

            return false;
        }
    }
}
