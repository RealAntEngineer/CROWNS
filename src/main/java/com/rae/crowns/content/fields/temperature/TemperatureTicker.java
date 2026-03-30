package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.fields.util.SectionLooper;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.rae.crowns.content.fields.util.PosPackingUtil.packSection;

/**
 * Temperature solver implemented with SectionLooper.
 * Uses TemperatureWorldData API (unchanged).
 */
public final class TemperatureTicker {
    public static int TICK_PERIOD = 1;
    public static float DT = TICK_PERIOD / 20f;
    public static float CAPACITY = 3e4f;

    public static void tick(@NotNull Set<Long> tickingSections, @NotNull PhysicsWorldData data) {

        data.resetTicked();

        updateDynamicData(data);

        // --- MAIN TICK LOOP ---
        TemperatureVoxelVisitor visitor = new TemperatureVoxelVisitor(data);
        SectionLooper.iterate(tickingSections, (sectionPos) ->
                        data.getLayers(
                                sectionPos,
                                DataLayerType.TEMPERATURE,
                                DataLayerType.DEFAULT_TEMPERATURE,
                                DataLayerType.CONDUCTION,
                                DataLayerType.RESILIENCE
                        )
                , visitor);
        data.setDirty();
    }

    private static void updateDynamicData(@NotNull PhysicsWorldData data) {
        // --- DYNAMIC DATA LOOP ---
        data.getDynamicData().forEach((key, value) -> {
            BlockPos pos = BlockPos.of(key);

            if (value instanceof BlockEntity blockEntity && blockEntity.isRemoved()) {
                return;
            }

            int sx = pos.getX() >> 4;
            int sy = pos.getY() >> 4;
            int sz = pos.getZ() >> 4;
            long packedSection = packSection(sx, sy, sz);

            TemperatureDataLayer temperatureData = data.getLayer(DataLayerType.TEMPERATURE, packedSection);
            TemperatureDataLayer defaultTemperatureData = data.getLayer(DataLayerType.DEFAULT_TEMPERATURE, packedSection);
            ConductionDataLayer conductionData = data.getLayer(DataLayerType.CONDUCTION, packedSection);
            ResilienceDataLayer resilienceData = data.getLayer(DataLayerType.RESILIENCE, packedSection);

            boolean corrupted = false;

            if (temperatureData == null) {
                CROWNS.LOGGER.warn("error trying to load temperature data at {}", SectionPos.of(packedSection));
                data.scheduleInitialisation(packedSection, DataLayerType.TEMPERATURE); //data got corrupted.
                corrupted = true;
            }
            if (defaultTemperatureData == null) {
                CROWNS.LOGGER.warn("error trying to load default temperature data at {}", SectionPos.of(packedSection));
                data.scheduleInitialisation(packedSection, DataLayerType.DEFAULT_TEMPERATURE); //data got corrupted.
                corrupted = true;
            }
            if (conductionData == null) {
                CROWNS.LOGGER.warn("error trying to load conduction data at {}", SectionPos.of(packedSection));
                data.scheduleInitialisation(packedSection, DataLayerType.CONDUCTION); //data got corrupted.
                corrupted = true;
            }
            if (resilienceData == null) {
                CROWNS.LOGGER.warn("error trying to load resilience data at {}", SectionPos.of(packedSection));
                data.scheduleInitialisation(packedSection, DataLayerType.RESILIENCE); //data got corrupted.
                corrupted = true;
            }
            if (corrupted) return;

            int lx = pos.getX() & 15;
            int ly = pos.getY() & 15;
            int lz = pos.getZ() & 15;

            float temp = value.getTemperature();
            temperatureData.set(lx, ly, lz, temp);
            defaultTemperatureData.set(lx, ly, lz, temp);
            resilienceData.set(lx, ly, lz, 0);
            conductionData.set(lx, ly, lz, value.getThermalConductivity());

            data.setDirty(packedSection);
        });
    }

    /**
     * A dedicated visitor class to handle voxel-level temperature simulation.
     * Eliminates lambdas and avoids AtomicReference overhead.
     */
    private static final class TemperatureVoxelVisitor implements SectionLooper.VoxelVisitor {

        private final PhysicsWorldData data;
        private final TemperatureNeighborVisitor neighborVisitor;


        public TemperatureVoxelVisitor(PhysicsWorldData data) {
            this.data = data;
            neighborVisitor = new TemperatureNeighborVisitor(data);
        }

        @Override
        public void visit(long packedSection, int sx, int sy, int sz, int x, int y, int z, SectionLooper.Context ctx) {
            if (x == 1 && y == 1 && z == 1) data.addToTicked(packedSection);
            long pos = ctx.packedPos();

            float selfTemp = ctx.getData(0);
            float selfDefaultTemp = ctx.getData(1);
            float selfCond = ctx.getData(2);
            float res = ctx.getData(3);

            // --- Prepare neighbor visitor ---
            neighborVisitor.setContext(ctx,
                    selfTemp, selfCond
            );

            // --- Run neighbor iteration ---
            ctx.forEachNeighbor(neighborVisitor);

            float totalFlux = neighborVisitor.totalFlux;

            float newTemp = (float) Mth.clamp(
                    selfTemp + ((selfDefaultTemp - selfTemp) * res * 1000d + totalFlux * (1 - res)) * DT / CAPACITY,
                    TemperatureDataLayer.MIN_TEMPERATURE, TemperatureDataLayer.MAX_TEMPERATURE
            );

            if ((newTemp != selfTemp || data.dynamicContains(pos))) {
                if (data.dynamicContains(pos)) {
                    IHaveTemperature be = data.getDynamic(pos);
                    be.addTemperature(newTemp - selfTemp);
                }
                ctx.setData(0, newTemp);

                if (Math.abs(selfTemp - newTemp) > 1e-3f) {
                    data.setDirty(packedSection);

                    // Mark cross-section dirty neighbors.
                    ctx.forEachNeighbor((nx, ny, nz, ref) -> {
                        if (ref.packedSection() != packedSection) {
                            data.setDirty(ref.packedSection());
                        }
                    });
                } else {
                    data.setClean(packedSection);
                }
            } else {
                data.setClean(packedSection);
            }
        }
    }

    private static final class TemperatureNeighborVisitor implements SectionLooper.Context.NeighborConsumer {
        private final PhysicsWorldData data;
        // Accumulated result
        public float totalFlux;
        // Shared inputs (set before each forEachNeighbor call)
        private float selfTemp, selfCond;
        private SectionLooper.Context ctx;


        public TemperatureNeighborVisitor(PhysicsWorldData data) {
            this.data = data;
        }

        public void setContext(SectionLooper.Context ctx, float selfTemp, float selfCond) {

            this.ctx = ctx;
            this.selfTemp = selfTemp;
            this.selfCond = selfCond;
            this.totalFlux = 0f;
        }

        @Override
        public void accept(int nx, int ny, int nz, SectionLooper.NeighborRef ref) {
            float neighborTemp;
            float neighborCond;
            if (ref.packedSection() != ctx.packedSectionPos()) {
                TemperatureDataLayer nTempLayer = data.getLayer(DataLayerType.TEMPERATURE, ref.packedSection());
                ConductionDataLayer nCondLayer = data.getLayer(DataLayerType.CONDUCTION, ref.packedSection());

                if (nTempLayer == null || nCondLayer == null)
                    return;

                neighborTemp = nTempLayer.get(ref.localX(), ref.localY(), ref.localZ());
                neighborCond = nCondLayer.get(ref.localX(), ref.localY(), ref.localZ());
            } else {
                neighborTemp = ctx.getData(0, nx, ny, nz);
                neighborCond = ctx.getData(2, nx, ny, nz);
            }
            float k = (neighborCond * selfCond) / (neighborCond + selfCond);

            float dTemp = neighborTemp - selfTemp;

            totalFlux += dTemp * (k);
        }
    }
}
