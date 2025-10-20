package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.data.PacketInit;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TemperatureWorldData {//Only for the server
    private final Long2ObjectMap<TemperatureDataLayer> temperatureMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ConductionDataLayer> conductionMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ResilienceDataLayer> resilienceMap = new Long2ObjectOpenHashMap<>();
    private final Map<Vec3i, IHaveTemperature> dynamicData = new HashMap<>();
    private final Queue<SectionPos> toInitialise = new ArrayDeque<>();
    //use to store changed positions so we don't encounter a deadlock
    private final Queue<BlockPos> changedBlocks = new ConcurrentLinkedQueue<>();

    private final LongSet changedSections = new LongOpenHashSet();
    private final LongSet dirty = new LongOpenHashSet();

    public Set<Long> getLoadedSections() {
        return new HashSet<>(temperatureMap.keySet()); // You can safely expose this if you're not modifying it
    }

    public TemperatureDataLayer getTemperature(long section) {
        return temperatureMap.get(section);
    }

    public ResilienceDataLayer getResilience(long section) {
        return resilienceMap.get(section);
    }

    public ConductionDataLayer getConduction(long section) {
        return conductionMap.get(section);
    }

    public void put(long section, TemperatureDataLayer dataLayer) {
        temperatureMap.put(section, dataLayer);
    }

    public void put(long section, ResilienceDataLayer dataLayer) {
        resilienceMap.put(section, dataLayer);
    }

    public void put(long section, ConductionDataLayer dataLayer) {
        conductionMap.put(section, dataLayer);
    }

    public void putForInitialisation(SectionPos section) {
        toInitialise.add(section);
        setDirty(section.asLong());
    }

    public void initialise(ServerLevel level) {
        // do the break with a timer.
        float initialTimeMS = System.currentTimeMillis();
        int range = CROWNSConfigs.SERVER.conduction.conductionLimitDistance.get();
        for (int i = 0; i < 10000 && !toInitialise.isEmpty(); i++) {
            if (System.currentTimeMillis() - initialTimeMS > 20) {
                break;
            }
            SectionPos sectionPos = toInitialise.peek();
            assert sectionPos != null;
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
                        defaultTemp = TemperatureManager.getDefaultTemperature(level, pos);
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
            temperatureMap.put(sectionPos.asLong(), temperatureDataLayer);
            conductionMap.put(sectionPos.asLong(), conductionDataLayer);
            resilienceMap.put(sectionPos.asLong(), resilienceDataLayer);
            if (!canBeDirty) {
                setClean(sectionPos.asLong());
            }

        }
    }

    public void updateChangedBlocks(ServerLevel level) {
        float initialTimeMS = System.currentTimeMillis();
        for (int i = 0; i < 10000 && !changedBlocks.isEmpty(); i++) {
            BlockPos pos = changedBlocks.poll();
            set(pos, TemperatureManager.getDefaultTemperature(level, pos), TemperatureManager.getDefaultConduction(level, pos), TemperatureManager.getDefaultResilience(level, pos));
            setDirty(SectionPos.of(pos).asLong());
            if (System.currentTimeMillis() - initialTimeMS > 20) {
                break;
            }
        }
    }

    public void set(BlockPos pos, float temperature, float conduction, float resilience) {
        // --- Compute packed section coordinates manually ---
        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;
        long packedSection = ((long) sx & 0xFFFFF) << 40 | ((long) sy & 0xFFFFF) << 20 | ((long) sz & 0xFFFFF);

        // --- Get layers ---
        TemperatureDataLayer temperatureDataLayer = getTemperature(packedSection);
        ConductionDataLayer conductionDataLayer = getConduction(packedSection);
        ResilienceDataLayer resilienceDataLayer = getResilience(packedSection);

        if (temperatureDataLayer != null && conductionDataLayer != null && resilienceDataLayer != null) {
            // --- Local coordinates inside the section ---
            int lx = pos.getX() & 15;
            int ly = pos.getY() & 15;
            int lz = pos.getZ() & 15;

            // --- Set values once ---
            temperatureDataLayer.set(lx, ly, lz, temperature);
            temperatureDataLayer.setDefault(lx, ly, lz, temperature);
            conductionDataLayer.set(lx, ly, lz, conduction);
            resilienceDataLayer.set(lx, ly, lz, resilience);

            // --- No need to put layers back if your map already stores references ---
            // put(packedSection, temperatureDataLayer);
            // put(packedSection, conductionDataLayer);
            // put(packedSection, resilienceDataLayer);
        }
    }

    //IHaveTemperature management

    public void putDynamic(BlockPos pos, IHaveTemperature dynamic) {
        dynamicData.put(pos, dynamic);
    }

    public Map<Vec3i, IHaveTemperature> getDynamicData() {
        return dynamicData;
    }

    public boolean dynamicContains(Vec3i pos) {
        return dynamicData.containsKey(pos);
    }

    public IHaveTemperature getDynamic(Vec3i pos) {
        return dynamicData.get(pos);
    }

    //to avoid ticking stable sections.
    public void setDirty(long sectionPos) {
        dirty.add(sectionPos);
        changedSections.add(sectionPos);
    }

    public boolean isDirty(long sectionPos) {
        return dirty.contains(sectionPos);
    }

    public void setClean(long sectionPos) {
        dirty.remove(sectionPos);
    }

    public void registerChanged(BlockPos immutable) {
        if (temperatureMap.containsKey(SectionPos.of(immutable))) {
            changedBlocks.add(immutable);
        }
    }


    public void syncWithPlayers(List<ServerPlayer> players) {
        // Grab the first 100 changed sections
        int batchSize = 10;

        // Only keep the entries that actually changed
        List<Map.Entry<SectionPos, TemperatureDataLayer>> entries = temperatureMap.entrySet().stream().filter(entry -> changedSections.contains(entry.getKey())).map(entry -> Map.entry(SectionPos.of(entry.getKey()), entry.getValue())).toList();
        //System.out.println("updated " + entries.size() + " sections to the client");

        for (int i = 0; i < entries.size(); i += batchSize) {
            // Create a batch of up to 10
            Map<SectionPos, TemperatureDataLayer> batch = entries.subList(i, Math.min(i + batchSize, entries.size())).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!batch.isEmpty() && !players.isEmpty()) {

                // Send to all players
                for (ServerPlayer player : players) {
                    PacketInit.getChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new UpdateSectionsPacket(batch));
                }

                // Remove them globally
                changedSections.removeAll(batch.keySet().stream().map(SectionPos::asLong).collect(Collectors.toSet()));
            }
        }
    }

    public void dumpSection(SectionPos sectionPos) {
        // Remove the section
        temperatureMap.remove(sectionPos);
        conductionMap.remove(sectionPos);
        resilienceMap.remove(sectionPos);

        // clean up other related queues/maps
        changedSections.remove(sectionPos.asLong());
        dirty.remove(sectionPos);
    }
}