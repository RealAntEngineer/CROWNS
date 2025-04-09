package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.init.data.AttachementTypeInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class TemperatureWorldData  {
    private final Map<ChunkPos, List<TemperatureDataLayer>> sectionMap = new HashMap<>();
    private final Queue<ChunkPos> toInitialise = new ArrayDeque<>();

    //TODO hook onto chunk serializer and do a packet for client server sync (always for server to client)

    public Set<ChunkPos> getLoadedSections() {
        return sectionMap.keySet(); // You can safely expose this if you're not modifying it
    }

    public List<TemperatureDataLayer> getOrCreate(ChunkPos section) {
        return sectionMap.computeIfAbsent(section, k -> new ArrayList<>());
    }

    public List<TemperatureDataLayer> getIfExists(ChunkPos section) {
        return sectionMap.get(section);
    }
    public void put(ChunkPos section, List<TemperatureDataLayer> temperatureDataLayer) {
        sectionMap.put(section, temperatureDataLayer);
    }
    public void putForInitialisation(ChunkPos section) {
        toInitialise.add(section);
    }
    public void initialise(ServerLevel level) {
        for (int i = 0; i < 10000 && !toInitialise.isEmpty();i++) {
            ChunkPos chunkPos  = toInitialise.poll();
            if (!level.isLoaded(chunkPos.getWorldPosition())) continue;
            int nbrOfSections = level.getChunk(chunkPos.getWorldPosition()).getSectionsCount();
            List<TemperatureDataLayer> sections = new ArrayList<>(nbrOfSections);
            for (int y = level.getChunk(chunkPos.getWorldPosition()).getMinSection(); y < level.getChunk(chunkPos.getWorldPosition()).getMaxSection(); y++) {
                TemperatureDataLayer temperatureDataLayer = new TemperatureDataLayer();
                BlockPos base = SectionPos.of(chunkPos,y).origin();
                for (int dx = 0; dx < 16; dx++) {
                    for (int dy = 0; dy < 16; dy++) {
                        for (int dz = 0; dz < 16; dz++) {
                            BlockPos pos = base.offset(dx, dy, dz);
                            temperatureDataLayer.set(dx, dy, dz, TemperatureManager.getDefaultTemperature(level, pos));
                        }
                    }
                }
                sections.add(temperatureDataLayer);
            }
            level.getChunk(chunkPos.getWorldPosition()).setData(AttachementTypeInit.CHUNK_TEMPERATURE.get(), sections);
            level.getChunk(chunkPos.getWorldPosition()).setUnsaved(true);
            sectionMap.put(chunkPos, sections);

        }
    }

    public float get(BlockPos pos) {
        TemperatureDataLayer data = getOrCreate(new ChunkPos(pos)).get(pos.getY() & 15);
        return data.get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    public void set(BlockPos pos, float value) {
        TemperatureDataLayer data = getOrCreate(new ChunkPos(pos)).get(pos.getY() & 15);
        data.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, value);
    }
}