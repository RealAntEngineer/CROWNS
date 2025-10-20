package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public class TemperatureTicker {
    public static int TICK_PERIOD = 1;
    public static float DT = TICK_PERIOD/20f;

    public static void tick(Set<SectionPos> loadedSections, TemperatureWorldData data) {
        //System.out.println("ticking for "+loadedSections.size()+" sections");
        //System.out.println("of "+data.getLoadedSections().size()+"in memory");
        List<Vec3i> toDump = new ArrayList<>();
        int range = CROWNSConfigs.SERVER.conduction.conductionLimitDistance.get();
        Set<SectionPos> sectionAccumulator = new HashSet<>(80);

        for (Map.Entry<Vec3i, IHaveTemperature> entry : data.getDynamicData().entrySet()) {
            Vec3i pos = entry.getKey();
            IHaveTemperature value = entry.getValue();

            if (value instanceof BlockEntity blockEntity && blockEntity.isRemoved()) {
                toDump.add(pos);
                continue;
            }

            // --- Compute section coords manually ---
            int sx = pos.getX() >> 4;
            int sy = pos.getY() >> 4;
            int sz = pos.getZ() >> 4;

            TemperatureDataLayer temperatureData = data.getTemperature(sx, sy, sz);
            ConductionDataLayer conductionData = data.getConduction(sx, sy, sz);
            ResilienceDataLayer resilienceData = data.getResilience(sx, sy, sz);

            if (temperatureData == null || conductionData == null || resilienceData == null) continue;

            // local coordinates inside the section
            int lx = pos.getX() & 15;
            int ly = pos.getY() & 15;
            int lz = pos.getZ() & 15;

            // update temperature and conduction
            float temp = value.getTemperature();
            temperatureData.set(lx, ly, lz, temp);
            temperatureData.setDefault(lx, ly, lz, temp);
            conductionData.set(lx, ly, lz, value.getThermalConductivity());

            // --- iterate neighbors without creating SectionPos objects ---
            for (int dx = -range; dx <= range; dx++) {
                int nsx = sx + dx;
                for (int dy = -range; dy <= range; dy++) {
                    int nsy = sy + dy;
                    for (int dz = -range; dz <= range; dz++) {
                        int nsz = sz + dz;

                        // Check Euclidean distance in section space
                        if (isInDynamicRange(pos, range).test(nsx, nsy, nsz)
                                && loadedSections.contains(nsx, nsy, nsz)) {
                            // Reuse SectionPos instance or use a lightweight hash key
                            sectionAccumulator.add(new SectionPos(nsx, nsy, nsz));
                        }
                    }
                }
            }

            data.setDirty(sx, sy, sz);
        }

        for (Vec3i pos : toDump){
            data.getDynamicData().remove(pos);
        }
        //System.out.println("limiting the originally loaded "+ loadedSections.size()+ " to only "+ sectionAccumulator.size()+ " sections");
        loadedSections = sectionAccumulator;
        //we should only tick this if we are allowed by the config.
        for (SectionPos sectionPos : loadedSections) {
            BlockPos base = sectionPos.origin();
            TemperatureDataLayer temperatureData = data.getTemperature(sectionPos);
            ConductionDataLayer conductionData = data.getConduction(sectionPos);
            ResilienceDataLayer resilienceData = data.getResilience(sectionPos);


            if (temperatureData == null || conductionData == null || resilienceData == null) continue;
            //TODO allow for conduction past the frontiers.
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = base.offset(x, y, z);
                        float selfDefaultTemp = temperatureData.getDefault(x, y, z);
                        float selfTemp = temperatureData.get(x, y, z);
                        float selfCond = conductionData.get(x, y, z) * DT;//nope we are going to do it with omega.
                        float weightedMean = 0;
                        float weights = 0;
                        float maxTemp = selfDefaultTemp;
                        float minTemp = selfDefaultTemp;
                        for (Direction dir : Direction.values()) {
                            int dx = dir.getStepX(), dy = dir.getStepY(), dz = dir.getStepZ();
                            if (0 < x + dx && x + dx < 16 && 0 < y + dy && y + dy < 16 && 0 < z + dz && z + dz < 16) {
                                float neighborTemp = temperatureData.get(x + dx, y + dy, z + dz);
                                float neighborDefaultTemp = temperatureData.getDefault(x + dx, y + dy, z + dz);
                                maxTemp = Math.max(neighborDefaultTemp, maxTemp);
                                minTemp = Math.min(neighborDefaultTemp, minTemp);
                                float neighborCond = conductionData.get(x + dx, y + dy, z + dz) * DT;

                                weightedMean += neighborTemp * (neighborCond * selfCond) / (neighborCond + selfCond);
                                weights += (neighborCond * selfCond) / (neighborCond + selfCond);
                            } else {
                                // Cross-section neighbor
                                BlockPos neighborPos = pos.relative(dir);
                                SectionPos neighborSection = SectionPos.of(neighborPos);
                                TemperatureDataLayer neighborTempData = data.getTemperature(neighborSection);
                                ConductionDataLayer neighborCondData = data.getConduction(neighborSection);

                                if (neighborTempData != null && neighborCondData != null) {
                                    int lx = neighborPos.getX() & 15;
                                    int ly = neighborPos.getY() & 15;
                                    int lz = neighborPos.getZ() & 15;

                                    float neighborTemp = neighborTempData.get(lx, ly, lz);
                                    float neighborCond = neighborCondData.get(lx, ly, lz) * DT;

                                    maxTemp = Math.max(neighborTemp, maxTemp);
                                    minTemp = Math.min(neighborTemp, minTemp);
                                    float blendWeight = (neighborCond * selfCond) / (neighborCond + selfCond);
                                    weightedMean += neighborTemp * blendWeight;
                                    weights += blendWeight;
                                }
                            }

                        }
                        //just to have access to the value in debug mode
                        float newTemp = Mth.clamp(
                                Mth.clamp((selfDefaultTemp - selfTemp)
                                        * resilienceData.get(x, y, z) + weightedMean / weights, minTemp, maxTemp),
                                TemperatureDataLayer.MIN_TEMPERATURE, TemperatureDataLayer.MAX_TEMPERATURE);
                        newTemp = (newTemp * 0.9f + selfTemp * 0.1f);//here to dampen oscillations
                        if ((newTemp != selfDefaultTemp || data.dynamicContains(pos))&& Mth.abs(selfTemp - newTemp) > 0.5f) {
                            if (data.dynamicContains(pos)) {
                                IHaveTemperature be = data.getDynamic(pos);
                                be.addTemperature(newTemp - selfTemp);
                            }
                            temperatureData.set(x, y, z, newTemp);
                            data.setDirty(sectionPos);

                            if (x == 0) data.setDirty(SectionPos.of(pos.west()));
                            if (x == 15) data.setDirty(SectionPos.of(pos.east()));
                            if (y == 0) data.setDirty(SectionPos.of(pos.below()));
                            if (y == 15) data.setDirty(SectionPos.of(pos.above()));
                            if (z == 0) data.setDirty(SectionPos.of(pos.north()));
                            if (z == 15) data.setDirty(SectionPos.of(pos.south()));
                        } else {
                            data.setClean(sectionPos);
                        }
                    }
                }
            }
        }
    }

    private static @NotNull Predicate<SectionPos> isInDynamicRange(Vec3i pos, int range) {
        return p -> p.center().distSqr(pos) < range * range * 16 * 16;
    }
}
