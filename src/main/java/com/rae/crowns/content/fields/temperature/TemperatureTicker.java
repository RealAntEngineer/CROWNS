package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.thermodynamics.conduction.IHaveTemperature;
import com.rae.crowns.init.data.AttachementTypeInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.stream.Collectors;

import static com.rae.crowns.content.fields.temperature.TemperatureManager.*;

public class TemperatureTicker {

    public static void tick(ServerLevel level) {
        TemperatureWorldData data = TemperatureManager.get(level);
        for (ChunkPos chunkPos : data.getLoadedSections().stream()
                .filter(pos -> level.isAreaLoaded(pos.getWorldPosition(),1))
                .collect(Collectors.toSet())) {
            List<TemperatureDataLayer> sections = data.getIfExists(chunkPos);
            int minSection = level.getChunk(chunkPos.getWorldPosition()).getMinSection();
            for (int y = minSection; y < level.getChunk(chunkPos.getWorldPosition()).getMaxSection(); y++) {
                BlockPos base = SectionPos.of(chunkPos,y).origin();
                TemperatureDataLayer sectionData = sections.get(y - minSection);
                if (sectionData == null) continue;

                for (int dx = 0; dx < 16; dx++) {
                    for (int dy = 0; dy < 16; dy++) {
                        for (int dz = 0; dz < 16; dz++) {
                            BlockPos pos = base.offset(dx, dy, dz);
                            BlockEntity selfBe = level.getBlockEntity(pos);
                            float defaultTemp = getDefaultTemperature(level, pos);
                            float selfTemp = sectionData.get(dx, dy, dz);

                            float selfCond;
                            float selfCap;

                            if (selfBe instanceof IHaveTemperature iSelf) {
                                selfTemp = iSelf.getTemperature();
                                selfCond = iSelf.getThermalConductivity();
                                selfCap = iSelf.getThermalCapacity();
                            } else {
                                selfCond = getBlockConduction(level, pos);
                                selfCap = getBlockCapacity(level, pos);
                            }
                            float transmited = 0;
                            for (Direction dir : Direction.values()) {
                                BlockPos neighborPos = pos.relative(dir);
                                BlockEntity neighborBe = level.getBlockEntity(neighborPos);

                                float neighborTemp, neighborCond;

                                if (neighborBe instanceof IHaveTemperature iNeighbor) {
                                    neighborTemp = (short) iNeighbor.getTemperature();
                                    neighborCond = iNeighbor.getThermalConductivity();

                                } else {
                                    // Fallback conduction using float map
                                    neighborTemp = sectionData.get(dx, dy, dz);  //todo set the fallback on load : getFallbackTemperature(level, neighborPos);
                                    neighborCond = getBlockConduction(level, neighborPos);
                                }
                                if (neighborTemp != selfTemp) {
                                    transmited += conductTemperature(neighborCond, selfCond, neighborTemp, selfTemp);
                                }
                            }
                            float lossRate = 0.01f; // tweak this constant
                            float totalPower = transmited - (float) ((Math.pow(selfTemp, 4) - Math.pow(defaultTemp, 4)) * lossRate);
                            if (totalPower != 0) {
                                if (selfBe instanceof IHaveTemperature iSelf) {
                                    iSelf.addTemperature(totalPower / selfCap);
                                }
                                sectionData.set(dx, dy, dz, selfTemp - totalPower / selfCap);

                            }
                        }
                    }
                }
            }
            level.getChunk(chunkPos.getWorldPosition()).setData(AttachementTypeInit.CHUNK_TEMPERATURE.get(), sections);
            level.getChunk(chunkPos.getWorldPosition()).setUnsaved(true);
        }
    }

    private static float conductTemperature(float neighborCond, float selfCond, float neighborTemp, float selfTemp) {
        float dt = 2f;
        float transmitted = 0f;

        if (neighborCond != 0 || selfCond != 0) {
            transmitted = (neighborTemp - selfTemp)
                    * (selfCond * neighborCond)
                    / (selfCond + neighborCond) * dt;
        }
        return transmitted;
    }


}


