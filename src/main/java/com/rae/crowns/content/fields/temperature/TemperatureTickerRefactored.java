package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Temperature solver implemented with SectionLooper.
 * Uses TemperatureWorldData API (unchanged).
 */
public final class TemperatureTickerRefactored {
    public static int TICK_PERIOD = 1;
    public static float DT = TICK_PERIOD / 20f;
    public static float CAPACITY = 3e4f;

    public static void tick(@NotNull Set<Long> tickingSections, @NotNull TemperatureWorldData data) {

        // --- DYNAMIC DATA LOOP (unchanged) ---
        data.getDynamicData().forEach( (key, value) -> {
            BlockPos pos = BlockPos.of(key);

            if (value instanceof BlockEntity blockEntity && blockEntity.isRemoved()) {
                return;
            }

            int sx = pos.getX() >> 4;
            int sy = pos.getY() >> 4;
            int sz = pos.getZ() >> 4;
            long packedSection = SectionLooper.packSection(sx, sy, sz);

            TemperatureDataLayer temperatureData = data.getTemperature(packedSection);
            TemperatureDataLayer defaultTemperatureData = data.getDefaultTemperature(packedSection);
            ConductionDataLayer conductionData = data.getConduction(packedSection);
            ResilienceDataLayer resilienceData = data.getResilience(packedSection);

            if (temperatureData == null || conductionData == null || resilienceData == null) {
                CROWNS.LOGGER.warn("error trying to load temperature data at {}", SectionPos.of(packedSection));
                data.putForInitialisation(packedSection); //data got corrupted.
                return;
            }

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

        // --- MAIN TICK LOOP ---
        TemperatureVoxelVisitor visitor = new TemperatureVoxelVisitor(data);
        SectionLooper.iterate(tickingSections,(sectionPos) ->
                new AbstractDataLayer[]{
                        data.getTemperature(sectionPos),
                        data.getDefaultTemperature(sectionPos),
                        data.getConduction(sectionPos),
                        data.getResilience(sectionPos)
                }, visitor);
    }

    /**
     * A dedicated visitor class to handle voxel-level temperature simulation.
     * Eliminates lambdas and avoids AtomicReference overhead.
     */
    private static final class TemperatureVoxelVisitor implements SectionLooper.VoxelVisitor {

        private final TemperatureWorldData data;

        public TemperatureVoxelVisitor(TemperatureWorldData data) {
            this.data = data;
        }

        @Override
        public void visit(long packedSection, int sx, int sy, int sz, int x, int y, int z, SectionLooper.Context ctx) {
            long pos = ctx.packedPos();

            float selfTemp = ctx.getData(0);
            float selfDefaultTemp = ctx.getData(1);
            float selfCond = ctx.getData(2);
            float res = ctx.getData(3);

            final float[] totalFlux = {0f};

            // --- NEIGHBORS ---
            ctx.forEachNeighbor((nx, ny, nz, ref) -> {
                // Only fetch from data if we're in a different section.
                TemperatureDataLayer nTempLayer = data.getTemperature(ref.packedSection());
                ConductionDataLayer nCondLayer = data.getConduction(ref.packedSection());

                if (nTempLayer == null || nCondLayer == null)
                    return; // skip missing neighbor data

                float neighborTemp = nTempLayer.get(ref.localX(), ref.localY(), ref.localZ());
                float neighborCond = nCondLayer.get(ref.localX(), ref.localY(), ref.localZ());

                float blend = (neighborCond * selfCond) / (neighborCond + selfCond);
                totalFlux[0] += (neighborTemp - selfTemp) * blend;
            });

            float newTemp = (float) Mth.clamp(
                    selfTemp + ((selfDefaultTemp - selfTemp) * res * 1000d + totalFlux[0] * (1 - res)) * DT / CAPACITY,
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
}
