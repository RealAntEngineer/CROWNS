package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.TickAccess;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TemperatureWorldData  {//Only for the server
    private final Map<SectionPos, TemperatureDataLayer> temperatureMap = new HashMap<>();
    private final Map<SectionPos, ConductionDataLayer> conductionMap = new HashMap<>();
    private final Map<SectionPos, ResilienceDataLayer> resilienceMap = new HashMap<>();

    private final Map<Vec3i, IHaveTemperature> dynamicData = new HashMap<>();
    private final Queue<SectionPos> toInitialise = new ArrayDeque<>();
    //use to store changed positions so we don't encounter a deadlock
    private final Queue<BlockPos> changedBlocks = new ConcurrentLinkedQueue<>();
    private final Set<SectionPos> dirty = new HashSet<>();
    //TODO hook onto chunk serializer and do a packet for client server sync (always for server to client)

    public Set<SectionPos> getLoadedSections() {
        return temperatureMap.keySet(); // You can safely expose this if you're not modifying it
    }

    public TemperatureDataLayer getTemperature(SectionPos section) {
        return temperatureMap.get(section);
    }
    public ResilienceDataLayer getResilience(SectionPos section) {
        return resilienceMap.get(section);
    }
    public ConductionDataLayer getConduction(SectionPos section) {
        return conductionMap.get(section);
    }
    public void put(SectionPos section, TemperatureDataLayer dataLayer) {
        temperatureMap.put(section, dataLayer);
    }
    public void put(SectionPos section, ResilienceDataLayer dataLayer) {
        resilienceMap.put(section, dataLayer);
    }
    public void put(SectionPos section, ConductionDataLayer dataLayer) {
        conductionMap.put(section, dataLayer);
    }

    public void putForInitialisation(SectionPos section) {
        toInitialise.add(section);
        setDirty(section);
    }
    public void initialise(ServerLevel level) {
        // do the break with a timer.
        float initialTimeMS = System.currentTimeMillis();
        for (int i = 0; i < 10000 && !toInitialise.isEmpty();i++) {
            SectionPos sectionPos  = toInitialise.peek();
            if (!level.isLoaded(sectionPos.origin())) continue;
            toInitialise.poll();
            TemperatureDataLayer temperatureDataLayer = new TemperatureDataLayer();
            ConductionDataLayer conductionDataLayer = new ConductionDataLayer();
            ResilienceDataLayer resilienceDataLayer = new ResilienceDataLayer();
            BlockPos base = sectionPos.origin();
            boolean canBeDirty = false;
            float defaultTemp = -1;
            for (int dx = 0; dx < 16; dx++) {
                for (int dy = 0; dy < 16; dy++) {
                    for (int dz = 0; dz < 16; dz++) {
                        BlockPos pos = base.offset(dx, dy, dz);
                        float oldTemp = defaultTemp;
                        defaultTemp = TemperatureManager.getDefaultTemperature(level,pos);
                        temperatureDataLayer.set(dx, dy, dz, defaultTemp);
                        temperatureDataLayer.setDefault(dx, dy, dz, defaultTemp);
                        conductionDataLayer.set(dx, dy, dz, TemperatureManager.getDefaultConduction(level, pos));
                        resilienceDataLayer.set(dx, dy, dz, TemperatureManager.getDefaultResilience(level, pos));
                        if (oldTemp != -1 && oldTemp != defaultTemp) {
                            canBeDirty = true;
                        }
                    }
                }
            }
            temperatureMap.put(sectionPos, temperatureDataLayer);
            conductionMap.put(sectionPos, conductionDataLayer);
            resilienceMap.put(sectionPos,resilienceDataLayer);
            if (!canBeDirty){
                setClean(sectionPos);
            }
            if (System.currentTimeMillis() - initialTimeMS > 20) {
                break;
            }
        }
    }
    public void updateChangedBlocks(ServerLevel level) {
        float initialTimeMS = System.currentTimeMillis();
        for (int i = 0; i < 10000 && !changedBlocks.isEmpty();i++) {
            BlockPos sectionPos  = changedBlocks.poll();
            set(sectionPos,
                    TemperatureManager.getDefaultTemperature(level, sectionPos),
                    TemperatureManager.getDefaultConduction(level, sectionPos),
                    TemperatureManager.getDefaultResilience(level, sectionPos));

            if (System.currentTimeMillis() - initialTimeMS > 20) {
                break;
            }
        }
    }
    public void set(BlockPos pos, float temperature, float conduction, float resilience) {
        SectionPos sectionPos = SectionPos.of(pos);
        TemperatureDataLayer temperatureDataLayer = getTemperature(sectionPos);
        ConductionDataLayer conductionDataLayer = getConduction(sectionPos);
        ResilienceDataLayer resilienceDataLayer = getResilience(sectionPos);

        if (temperatureDataLayer != null && conductionDataLayer != null) {
            temperatureDataLayer.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, temperature);
            temperatureDataLayer.setDefault(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, temperature);
            conductionDataLayer.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, conduction);
            resilienceDataLayer.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, resilience);

            put(sectionPos, temperatureDataLayer);
            put(sectionPos, conductionDataLayer);
        }

    }

    //IHaveTemperature management

    public void putDynamic(BlockPos pos, IHaveTemperature dynamic) {
        dynamicData.put(pos, dynamic);
    }

    public Map<Vec3i, IHaveTemperature> getDynamicData() {
        return dynamicData;
    }

    public boolean dynamicContains(Vec3i pos){
        return dynamicData.containsKey(pos);
    }
    public IHaveTemperature getDynamic(Vec3i pos){
        return dynamicData.get(pos);
    }
    //to avoid ticking stable sections.
    public void setDirty(SectionPos sectionPos) {
        dirty.add(sectionPos);
    }
    public boolean isDirty(SectionPos sectionPos){
        return dirty.contains(sectionPos);
    }

    public void setClean(SectionPos sectionPos) {
        dirty.remove(sectionPos);
    }

    public void registerChanged(BlockPos immutable) {
        if (temperatureMap.containsKey(SectionPos.of(immutable))) {
            changedBlocks.add(immutable);
        }
    }

}