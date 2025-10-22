package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.CROWNS;
import com.rae.crowns.config.CROWNSConfigs;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.data.PacketInit;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static com.rae.crowns.content.fields.temperature.TemperatureTicker.packSection;

public class TemperatureWorldData {//Only for the server

    //in the future hook into ChunkSection directly : easier for communication and initialisation

    private static final int DYNAMIC_RANGE = 2;
    private final Long2ObjectMap<TemperatureDataLayer> temperatureMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ConductionDataLayer> conductionMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ResilienceDataLayer> resilienceMap = new Long2ObjectOpenHashMap<>();
    private final Map<Vec3i, IHaveTemperature> dynamicData = new HashMap<>();
    private final LongSet toInitialise = new LongOpenHashSet();
    //use to store changed positions so we don't encounter a deadlock
    private final Queue<BlockPos> changedBlocks = new ConcurrentLinkedQueue<>();

    private final LongSet changedSections = new LongOpenHashSet();
    private final LongSet dirty = new LongOpenHashSet();
    private final LongSet loadedSections = new LongOpenHashSet();
    // Map section -> number of dynamic blocks affecting it
    private final Long2IntMap sectionDynamicCount = new Long2IntOpenHashMap();
    private final LongSet nearDynamicSections = new LongOpenHashSet();

    public LongSet getNearDynamic() {
        return nearDynamicSections; // You can safely expose this if you're not modifying it
    }

    public LongSet getLoadedSections(){
        return loadedSections;
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

    public void put(long section, TemperatureDataLayer tempDataLayer, ConductionDataLayer condDataLayer, ResilienceDataLayer resDataLayer) {
        temperatureMap.put(section, tempDataLayer);
        resilienceMap.put(section, resDataLayer);
        conductionMap.put(section, condDataLayer);

        loadedSections.add(section);
    }

    public void putForInitialisation(long section) {
        if (toInitialise.contains(section)) return;
        toInitialise.add(section);
        setDirty(section);
    }

    public void initialise(ServerLevel level) {
        long startTime = System.nanoTime(); // More accurate timing
        int processed = 0;

        // Use an iterator so we can remove safely
        LongIterator iterator = toInitialise.iterator();

        while (iterator.hasNext() && processed < 10000) {
            if ((System.nanoTime() - startTime) > 20_000_000L) { // 20 ms in nanoseconds
                CROWNS.LOGGER.warn("Exiting initialisation for this tick with {} more Sections to go", toInitialise.size());
                break;
            }

            long section = iterator.nextLong();
            SectionPos sectionPos = SectionPos.of(section);

            // ⚠️ Skip this section if not loaded, keep it in the set for later
            if (!level.isLoaded(sectionPos.origin())) {
                continue;
            }

            // ✅ Only remove once we're actually processing it
            iterator.remove();

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

            long sectionLong = sectionPos.asLong();
            temperatureMap.put(sectionLong, temperatureDataLayer);
            conductionMap.put(sectionLong, conductionDataLayer);
            resilienceMap.put(sectionLong, resilienceDataLayer);
            loadedSections.add(sectionLong);

            if (!canBeDirty && !nearDynamicSections.contains(sectionLong)) {
                setClean(sectionLong);
            }

            processed++;
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
        long packedSection = packSection(sx, sy, sz);

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

        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;

        for (int dx = -DYNAMIC_RANGE; dx <= DYNAMIC_RANGE; dx++) {
            int nsx = sx + dx;
            for (int dy = -DYNAMIC_RANGE; dy <= DYNAMIC_RANGE; dy++) {
                int nsy = sy + dy;
                for (int dz = -DYNAMIC_RANGE; dz <= DYNAMIC_RANGE; dz++) {
                    int nsz = sz + dz;

                    if (isInDynamicRange(pos, nsx, nsy, nsz)) {
                        long packed = packSection(nsx, nsy, nsz);
                        nearDynamicSections.add(packed);
                        sectionDynamicCount.put(packed, sectionDynamicCount.getOrDefault(packed, 0) + 1);
                    }
                }
            }
        }
    }

    public void removeDynamic(BlockPos pos) {
        dynamicData.remove(pos);

        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;
        int range = 2;

        for (int dx = -DYNAMIC_RANGE; dx <= DYNAMIC_RANGE; dx++) {
            int nsx = sx + dx;
            for (int dy = -DYNAMIC_RANGE; dy <= DYNAMIC_RANGE; dy++) {
                int nsy = sy + dy;
                for (int dz = -DYNAMIC_RANGE; dz <= DYNAMIC_RANGE; dz++) {
                    int nsz = sz + dz;

                    if (isInDynamicRange(pos, nsx, nsy, nsz)) {
                        long packed = packSection(nsx, nsy, nsz);
                        int count = sectionDynamicCount.getOrDefault(packed, 0) - 1;
                        if (count <= 0) {
                            sectionDynamicCount.remove(packed);
                            nearDynamicSections.remove(packed);
                        } else {
                            sectionDynamicCount.put(packed, count);
                        }
                    }
                }
            }
        }
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
        if (temperatureMap.containsKey(SectionPos.of(immutable).asLong())) {
            changedBlocks.add(immutable);
        }
    }


    public void syncWithPlayers(List<ServerPlayer> players) {
        // Grab the first 100 changed sections
        int batchSize = 10;

        // Only keep the entries that actually changed
        List<Map.Entry<SectionPos, TemperatureDataLayer>> entries = temperatureMap.long2ObjectEntrySet()
                .stream()
                .filter(entry -> changedSections.contains(entry.getLongKey()))
                .map(entry -> Map.entry(SectionPos.of(entry.getLongKey()), entry.getValue())).toList();
        //System.out.println("updated " + entries.size() + " sections to the client");

        for (int i = 0; i < entries.size(); i += batchSize) {
            // Create a batch of up to 10
            Map<SectionPos, TemperatureDataLayer> batch = entries.subList(i, Math.min(i + batchSize, entries.size())).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!batch.isEmpty() && !players.isEmpty()) {

                // Send to all players
                for (ServerPlayer player : players) {
                    //here we need to drop chunks that are too far.
                    PacketInit.getChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new UpdateSectionsPacket(batch));
                }

                // Remove them globally
                changedSections.removeAll(batch.keySet().stream().map(SectionPos::asLong).collect(Collectors.toSet()));
            }
        }
    }

    public void dumpSection(long sectionPos) {
        // Remove the section
        temperatureMap.remove(sectionPos);
        conductionMap.remove(sectionPos);
        resilienceMap.remove(sectionPos);
        loadedSections.remove(sectionPos);

        // clean up other related queues/maps
        //changedSections.remove(sectionPos);
        dirty.remove(sectionPos);
    }

    // --- HELPER FOR DYNAMIC RANGE CHECK ---
    private static boolean isInDynamicRange(Vec3i pos, int sx, int sy, int sz) {
        //block pos
        final int px = pos.getX();
        final int py = pos.getY();
        final int pz = pos.getZ();

        //section pos
        int dx = (sx << 4) + 8 - px;
        int dy = (sy << 4) + 8 - py;
        int dz = (sz << 4) + 8 - pz;
        return dx * dx + dy * dy + dz * dz < DYNAMIC_RANGE * DYNAMIC_RANGE * 16 * 16;
    }

    public void unloading(long pos) {
        loadedSections.remove(pos);
    }

    public boolean isLoaded(long pos) {
        return loadedSections.contains(pos);
    }
}