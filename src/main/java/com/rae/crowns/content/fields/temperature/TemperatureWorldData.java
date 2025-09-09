package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.misc.PacketInit;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TemperatureWorldData  {//Only for the server
    //private final Map<SectionPos, TemperatureDataLayer> temperatureMap = new HashMap<>();
    //private final Map<SectionPos, ConductionDataLayer> conductionMap = new HashMap<>();
    //private final Map<SectionPos, ResilienceDataLayer> resilienceMap = new HashMap<>();
    private final Long2ObjectOpenHashMap<TemperatureDataLayer> temperatureMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ConductionDataLayer> conductionMap   = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ResilienceDataLayer> resilienceMap   = new Long2ObjectOpenHashMap<>();
    private final Map<Vec3i, IHaveTemperature> dynamicData = new HashMap<>();
    private final Queue<SectionPos> toInitialise = new ArrayDeque<>();
    //use to store changed positions so we don't encounter a deadlock
    private final Queue<BlockPos> changedBlocks = new ConcurrentLinkedQueue<>();

    private final LongSet changedSections = new LongOpenHashSet();
    private final Set<SectionPos> dirty = new HashSet<>();
    //TODO hook onto chunk serializer and do a packet for client server sync (always for server to client)

    public Set<SectionPos> getLoadedSections() {
        return temperatureMap.keySet().stream().map(SectionPos::of).collect(Collectors.toSet()); // You can safely expose this if you're not modifying it
    }
    public TemperatureDataLayer getTemperature(SectionPos section) {
        return temperatureMap.get(section.asLong());
    }
    public ResilienceDataLayer getResilience(SectionPos section) {
        return resilienceMap.get(section.asLong());
    }
    public ConductionDataLayer getConduction(SectionPos section) {
        return conductionMap.get(section.asLong());
    }
    public void put(SectionPos section, TemperatureDataLayer dataLayer) {
        temperatureMap.put(section.asLong(), dataLayer);
    }
    public void put(SectionPos section, ResilienceDataLayer dataLayer) {
        resilienceMap.put(section.asLong(), dataLayer);
    }
    public void put(SectionPos section, ConductionDataLayer dataLayer) {
        conductionMap.put(section.asLong(), dataLayer);
    }

    public void putForInitialisation(SectionPos section) {
        toInitialise.add(section);
        setDirty(section);
    }
    public void initialise(ServerLevel level) {
        // do the break with a timer.
        float initialTimeMS = System.currentTimeMillis();
        int range = CROWNSConfigs.SERVER.conduction.conductionLimitDistance.get();
        for (int i = 0; i < 10000 && !toInitialise.isEmpty();i++) {
            if (System.currentTimeMillis() - initialTimeMS > 20) {
                break;
            }
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
            temperatureMap.put(sectionPos.asLong(), temperatureDataLayer);
            conductionMap.put(sectionPos.asLong(), conductionDataLayer);
            resilienceMap.put(sectionPos.asLong(),resilienceDataLayer);
            if (!canBeDirty){
                setClean(sectionPos);
            }

        }
    }

    private boolean inDynamicRange(SectionPos sectionPos,int range) {
        AtomicBoolean flag = new AtomicBoolean(false);
        dynamicData.keySet().forEach(blockPos -> {
             flag.set(sectionPos.center().distSqr(blockPos) < range * range * 64 || flag.get());
        });
        return flag.get();
    }

    public void updateChangedBlocks(ServerLevel level) {
        float initialTimeMS = System.currentTimeMillis();
        for (int i = 0; i < 10000 && !changedBlocks.isEmpty();i++) {
            BlockPos pos  = changedBlocks.poll();
            set(pos,
                    TemperatureManager.getDefaultTemperature(level, pos),
                    TemperatureManager.getDefaultConduction(level, pos),
                    TemperatureManager.getDefaultResilience(level, pos));
            setDirty(SectionPos.of(pos));
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

        if (temperatureDataLayer != null && conductionDataLayer != null && resilienceDataLayer != null) {
            temperatureDataLayer.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, temperature);
            temperatureDataLayer.setDefault(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, temperature);
            conductionDataLayer.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, conduction);
            resilienceDataLayer.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, resilience);

            put(sectionPos, temperatureDataLayer);
            put(sectionPos, conductionDataLayer);
            put(sectionPos, resilienceDataLayer);
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
        changedSections.add(sectionPos.asLong());
    }
    public boolean isDirty(SectionPos sectionPos){
        return dirty.contains(sectionPos);
    }

    public void setClean(SectionPos sectionPos) {
        dirty.remove(sectionPos);
    }

    public void registerChanged(BlockPos immutable) {
        if (temperatureMap.containsKey(SectionPos.of(immutable).asLong())) {
            changedBlocks.add(immutable);
        }
    }


    public void syncWithPlayers(List<ServerPlayer> players) {
        // Grab the first 100 changed sections
        int batchSize = 10;

        // Only keep the entries that actually changed
        List<Map.Entry<SectionPos, TemperatureDataLayer>> entries =
                temperatureMap.long2ObjectEntrySet().stream()
                        .filter(entry -> changedSections.contains(entry.getLongKey()))
                        .map(entry -> Map.entry(SectionPos.of(entry.getLongKey()), entry.getValue()))
                        .toList();
        System.out.println("updated " + entries.size() + " sections to the client");

        for (int i = 0; i < entries.size(); i += batchSize) {
            // Create a batch of up to 10
            Map<SectionPos, TemperatureDataLayer> batch = entries.subList(i, Math.min(i + batchSize, entries.size()))
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!batch.isEmpty() && !players.isEmpty()) {

                // Send to all players
                for (ServerPlayer player : players) {
                    PacketInit.getChannel().send(
                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new UpdateSectionsPacket(batch)
                    );
                }

                // Remove them globally
                changedSections.removeAll(batch.keySet().stream().map(SectionPos::asLong).collect(Collectors.toSet()));
            }
        }
    }

}