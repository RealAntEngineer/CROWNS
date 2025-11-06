package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.CROWNS;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TemperatureTicker {
    public static int TICK_PERIOD = 1;
    public static float DT = TICK_PERIOD / 20f;
    public static float CAPACITY = 3e4f;

    // --- PACKING SECTION COORDINATES ---
    public static long packSection(int sx, int sy, int sz) {
        return ((long)sx & 0x3FFFFF) << 42
                | ((long)sz & 0x3FFFFF) << 20
                | ((long)sy & 0xFFFFF);
    }

    public static int unpackSectionX(long packed) { return (int)(packed >> 42); }
    public static int unpackSectionY(long packed) { return (int)(packed << 44 >> 44); }
    public static int unpackSectionZ(long packed) { return (int)(packed << 22 >> 42); }


    public static void tick(@NotNull Set<Long> tickingSections, @NotNull TemperatureWorldData data) {
        //List<Vec3i> toDump = new ArrayList<>();

        // --- DYNAMIC DATA LOOP ---
        data.getDynamicData().forEach( (key, value) -> {
            BlockPos pos = BlockPos.of(key);
            if (value instanceof BlockEntity blockEntity && blockEntity.isRemoved()) {
                //toDump.add(pos);
                return;
            }

            int sx = pos.getX() >> 4;
            int sy = pos.getY() >> 4;
            int sz = pos.getZ() >> 4;
            long packedSection = packSection(sx, sy, sz);

            TemperatureDataLayer temperatureData = data.getTemperature(packedSection);
            TemperatureDataLayer defaultTemperatureData = data.getDefaultTemperature(packedSection);
            ConductionDataLayer conductionData = data.getConduction(packedSection);
            ResilienceDataLayer resilienceData = data.getResilience(packedSection);

            if (temperatureData == null || conductionData == null || resilienceData == null) {
                CROWNS.LOGGER.warn("error trying to load temperature data at {}", SectionPos.of(packedSection));
                data.putForInitialisation(packedSection);//this means that the data got corrupted.
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

        // --- MAIN TICK LOOP FOR SECTIONS ---
        for (long packedSection : tickingSections) {
            int sx = unpackSectionX(packedSection);
            int sy = unpackSectionY(packedSection);
            int sz = unpackSectionZ(packedSection);
            BlockPos base = new BlockPos(sx << 4, sy << 4, sz << 4);

            TemperatureDataLayer temperatureData = data.getTemperature(packedSection);
            TemperatureDataLayer defaultTemperatureData = data.getDefaultTemperature(packedSection);
            ConductionDataLayer conductionData = data.getConduction(packedSection);
            ResilienceDataLayer resilienceData = data.getResilience(packedSection);

            if (temperatureData == null || conductionData == null || resilienceData == null) continue;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = base.offset(x, y, z);

                        float selfDefaultTemp = defaultTemperatureData.get(x, y, z);
                        float selfTemp = temperatureData.get(x, y, z);
                        float selfCond = conductionData.get(x, y, z);

                        float totalFlux = 0;
                        //float weights = 0;
                        float maxTemp = selfTemp;
                        float minTemp = selfTemp;

                        // --- NEIGHBORS ---
                        for (Direction dir : Direction.values()) {
                            int nx = x + dir.getStepX();
                            int ny = y + dir.getStepY();
                            int nz = z + dir.getStepZ();
                            //this changed compared to previous version check if it's still valid (I don't think it's right.)
                            if (0 <= nx && nx < 16 && 0 <= ny && ny < 16 && 0 <= nz && nz < 16) {
                                float neighborTemp = temperatureData.get(nx, ny, nz);
                                float neighborCond = conductionData.get(nx, ny, nz);

                                maxTemp = Math.max(neighborTemp, maxTemp);
                                minTemp = Math.min(neighborTemp, minTemp);

                                float blend = (neighborCond * selfCond) / (neighborCond + selfCond);
                                totalFlux += (neighborTemp - selfTemp) * blend;
                            } else {
                                // --- CROSS-SECTION NEIGHBORS ---
                                int nsx = sx, nsy = sy, nsz = sz;
                                int lx = nx, ly = ny, lz = nz;

                                if (nx < 0) {
                                    nsx--;
                                    lx += 16;
                                } else if (nx >= 16) {
                                    nsx++;
                                    lx -= 16;
                                }

                                if (ny < 0) {
                                    nsy--;
                                    ly += 16;
                                } else if (ny >= 16) {
                                    nsy++;
                                    ly -= 16;
                                }

                                if (nz < 0) {
                                    nsz--;
                                    lz += 16;
                                } else if (nz >= 16) {
                                    nsz++;
                                    lz -= 16;
                                }

                                long neighborPacked = packSection(nsx, nsy, nsz);
                                TemperatureDataLayer neighborTempData = data.getTemperature(neighborPacked);
                                ConductionDataLayer neighborCondData = data.getConduction(neighborPacked);

                                if (neighborTempData != null && neighborCondData != null) {
                                    float neighborTemp = neighborTempData.get(lx, ly, lz);
                                    float neighborCond = neighborCondData.get(lx, ly, lz);

                                    maxTemp = Math.max(neighborTemp, maxTemp);
                                    minTemp = Math.min(neighborTemp, minTemp);
                                    //conduction bwn neighbor.
                                    float blend = (neighborCond * selfCond) / (neighborCond + selfCond);
                                    totalFlux += (neighborTemp - selfTemp) * blend;
                                    //weights += blend;
                                }
                            }
                        }

                        float res = resilienceData.get(x, y, z);
                        float newTemp =
                                (float) Mth.clamp(
                                        selfTemp + //
                                                ((selfDefaultTemp - selfTemp) * res * 1000d //ground callback
                                                + totalFlux * (1- res))* DT/CAPACITY,//fluxes sums.
                            /*Mth.clamp((selfDefaultTemp - selfTemp) * resilienceData.get(x, y, z) + totalFlux / weights,
                                    minTemp, maxTemp),*/
                            TemperatureDataLayer.MIN_TEMPERATURE, TemperatureDataLayer.MAX_TEMPERATURE);

                        //newTemp = newTemp * 0.9f + selfTemp * 0.1f;

                        if ((newTemp != selfTemp || data.dynamicContains(pos.asLong()))) {
                            if (data.dynamicContains(pos.asLong())) {
                                IHaveTemperature be = data.getDynamic(pos.asLong());
                                be.addTemperature(newTemp - selfTemp);
                            }
                            temperatureData.set(x, y, z, newTemp);
                            if (Mth.abs(selfTemp - newTemp) > 1e-3f) {
                                data.setDirty(packedSection);

                                // --- MARK CROSS-SECTION DIRTY SECTIONS ---
                                for (Direction dir : Direction.values()) {
                                    int nx = x + dir.getStepX();
                                    int ny = y + dir.getStepY();
                                    int nz = z + dir.getStepZ();

                                    if (nx < 0 || nx >= 16 || ny < 0 || ny >= 16 || nz < 0 || nz >= 16) {
                                        int nsx = sx + (nx < 0 ? -1 : nx >= 16 ? 1 : 0);
                                        int nsy = sy + (ny < 0 ? -1 : ny >= 16 ? 1 : 0);
                                        int nsz = sz + (nz < 0 ? -1 : nz >= 16 ? 1 : 0);
                                        long neighborPacked = packSection(nsx, nsy, nsz);
                                        data.setDirty(neighborPacked);
                                    }
                                }
                            } else {
                                data.setClean(packedSection);
                            }
                        } else {
                            data.setClean(packedSection);
                        }
                    }
                }
            }
        }
    }
}
