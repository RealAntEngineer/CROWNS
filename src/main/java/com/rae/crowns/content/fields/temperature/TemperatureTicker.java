package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.thermodynamics.conduction.IHaveTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemperatureTicker {
    public static float DT = 0.25f;


    public static void tick(Set<SectionPos> loadedSections, TemperatureWorldData data) {
        //System.out.println("ticking for "+loadedSections.size()+" sections");
        //System.out.println("of "+data.getLoadedSections().size()+"in memory");
        List<Vec3i> toDump = new ArrayList<>();
        for (Map.Entry<Vec3i,IHaveTemperature> entries:data.getDynamicData().entrySet()){
            Vec3i pos = entries.getKey();
            IHaveTemperature value = entries.getValue();
            if (value instanceof BlockEntity blockEntity){
                if (blockEntity.isRemoved()){
                    toDump.add(pos);
                    continue;
                }
            }

            SectionPos sectionPos = SectionPos.of((BlockPos) pos);
            TemperatureDataLayer temperatureData = data.getTemperature(sectionPos);
            ConductionDataLayer conductionData = data.getConduction(sectionPos);
            ResilienceDataLayer resilienceData = data.getResilience(sectionPos);

            if (temperatureData == null || conductionData == null || resilienceData == null) continue;
            temperatureData.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, value.getTemperature());
            temperatureData.setDefault(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, value.getTemperature());

            conductionData.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, value.getThermalConductivity());
            data.setDirty(sectionPos);
        }
        for (Vec3i pos : toDump){
            data.getDynamicData().remove(pos);
        }
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
                        float resilience = resilienceData.get(x, y, z);//just to have access to the value in debug mode
                        float newTemp = Mth.clamp((selfDefaultTemp - selfTemp) * resilienceData.get(x, y, z) + weightedMean / weights, minTemp, maxTemp);
                        if (newTemp != selfDefaultTemp) {
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
}
