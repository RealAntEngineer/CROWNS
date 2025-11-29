package com.rae.crowns.content.fields.util;

public class NeighborVisitors {

    /**
     * Neighbor consumer that computes first- and second-order
     * derivatives (central differences) of a scalar field.
     *
     * Produces ∂f/∂x, ∂f/∂y, ∂f/∂z and ∂²f/∂x², ∂²f/∂y², ∂²f/∂z²
     * from 6-neighbor samples.
     */
    public static final class ScalarDerivativeNeighborVisitor implements SectionLooper.Context.NeighborConsumer {
        private final PhysicsWorldData data;
        private final DataLayerType<? extends AbstractDataLayer> layerType;

        // context
        long packedSection;
        int x, y, z;
        float selfValue;

        // accumulators
        float dfdx, dfdy, dfdz;      // first derivatives
        float d2fdx2, d2fdy2, d2fdz2; // second derivatives
        boolean xp, xn, yp, yn, zp, zn; // which directions were found

        public ScalarDerivativeNeighborVisitor(PhysicsWorldData data, DataLayerType<? extends AbstractDataLayer> type) {
            this.data = data;
            this.layerType = type;
        }

        public void setContext(long packedSection, int x, int y, int z, float selfValue) {
            this.packedSection = packedSection;
            this.x = x;
            this.y = y;
            this.z = z;
            this.selfValue = selfValue;

            this.dfdx = this.dfdy = this.dfdz = 0f;
            this.d2fdx2 = this.d2fdy2 = this.d2fdz2 = 0f;
            this.xp = this.xn = this.yp = this.yn = this.zp = this.zn = false;
        }

        @Override
        public void accept(int nx, int ny, int nz, SectionLooper.NeighborRef ref) {
            int dx = nx - x;
            int dy = ny - y;
            int dz = nz - z;

            AbstractDataLayer nLayer = data.getLayer(layerType, ref.packedSection());
            if (nLayer == null) return;

            float nv = nLayer.get(ref.localX(), ref.localY(), ref.localZ());

            // Identify axis and accumulate
            if (dx == 1 && dy == 0 && dz == 0) {  // +X
                dfdx += nv;
                xp = true;
                d2fdx2 += nv;
            } else if (dx == -1 && dy == 0 && dz == 0) { // -X
                dfdx -= nv;
                xn = true;
                d2fdx2 += nv;
            } else if (dy == 1 && dx == 0 && dz == 0) { // +Y
                dfdy += nv;
                yp = true;
                d2fdy2 += nv;
            } else if (dy == -1 && dx == 0 && dz == 0) { // -Y
                dfdy -= nv;
                yn = true;
                d2fdy2 += nv;
            } else if (dz == 1 && dx == 0 && dy == 0) { // +Z
                dfdz += nv;
                zp = true;
                d2fdz2 += nv;
            } else if (dz == -1 && dx == 0 && dy == 0) { // -Z
                dfdz -= nv;
                zn = true;
                d2fdz2 += nv;
            }
        }

        /**
         * Finalizes derivative estimates after all neighbors were visited.
         * Uses spacing h = 1.
         */
        public float[] getResult() {
            float[] out = new float[6]; // [dfdx, dfdy, dfdz, d2fdx2, d2fdy2, d2fdz2]
            if (xp && xn) out[0] = 0.5f * dfdx; // central difference
            else out[0] = 0f;
            if (yp && yn) out[1] = 0.5f * dfdy;
            else out[1] = 0f;
            if (zp && zn) out[2] = 0.5f * dfdz;
            else out[2] = 0f;

            if (xp && xn) out[3] = (d2fdx2 - 2f * selfValue);
            if (yp && yn) out[4] = (d2fdy2 - 2f * selfValue);
            if (zp && zn) out[5] = (d2fdz2 - 2f * selfValue);

            return out;
        }
    }
}
