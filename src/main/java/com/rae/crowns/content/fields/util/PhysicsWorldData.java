package com.rae.crowns.content.fields.util;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NonnullDefault;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

@NonnullDefault
public class PhysicsWorldData extends SavedData {//Only for the server

    //in the future hook into ChunkSection directly : easier for communication and initialization

    public static final  int                                                       DATA_VERSION        = 13;
    private static final int                                                       DYNAMIC_RANGE       = 1;
    // Generic unified map: one Long2ObjectMap per DataLayerType
    private final        EnumMap<DataLayerType, Long2ObjectMap<AbstractDataLayer>> layers              = new EnumMap<>(DataLayerType.class);//stored
    // Dynamic and meta state
    private final        Long2ObjectMap<DataLayerType[]>                           toInitialise        = new Long2ObjectOpenHashMap<>();//stored
    private final        Queue<BlockPos>                                           changedBlocks       = new ConcurrentLinkedQueue<>();//stored
    private final        LongSet                                                   changedSections     = new LongOpenHashSet();//stored
    private final        LongSet                                                   dirty               = new LongOpenHashSet();//stored
    private final        LongSet                                                   loadedSections      = new LongOpenHashSet();//stored
    private final        Long2IntMap                                               tickedSections      = new Long2IntOpenHashMap();//recomputed
    private final        Long2ObjectMap<IHaveTemperature>                          dynamicData         = new Long2ObjectOpenHashMap<>();//recomputed
    private final        Long2IntMap                                               sectionDynamicCount = new Long2IntOpenHashMap();//recomputed
    private final        LongSet                                                   nearDynamicSections = new LongOpenHashSet();//recomputed
    //matrix
    private final HashMap<AbstractMatrixPhysicsSolver<?>, AbstractMatrixPhysicsSolver.PhysicsMatrix> cachedMatrices = new HashMap<>();
    private              int                                                       currentTime         = -1;//recomputed

    public static PhysicsWorldData loadData(ServerLevel server) {
        return server.getDataStorage()
                .computeIfAbsent(new Factory<>(PhysicsWorldData::new, (c, p) -> PhysicsWorldData.load(c)), "thermal_grid");
    }

    public PhysicsWorldData() {
        // Register default layer maps — any future DataLayerType will also work
        registerLayer(DataLayerType.TEMPERATURE);
        registerLayer(DataLayerType.DEFAULT_TEMPERATURE);
        registerLayer(DataLayerType.CONDUCTION);
        registerLayer(DataLayerType.RESILIENCE);
    }

    public static PhysicsWorldData load(CompoundTag nbt) {
        PhysicsWorldData data = new PhysicsWorldData();

        if (!nbt.contains("DataLayerVersion") ||
                nbt.getLong("DataLayerVersion") != DATA_VERSION) {
            return data;
        }

        if (nbt.contains("layers", Tag.TAG_COMPOUND)) {
            data.layers.putAll(deserializeLayers(nbt.getCompound("layers")));
        }

        if (nbt.contains("changedBlocks", Tag.TAG_LONG_ARRAY)) {
            for (long l : nbt.getLongArray("changedBlocks")) {
                data.changedBlocks.add(BlockPos.of(l));
            }
        }

        if (nbt.contains("toInitialise", Tag.TAG_COMPOUND)) {
            data.toInitialise.putAll(deserializeInit(nbt.getCompound("toInitialise")));
        }

        if (nbt.contains("changedSections", Tag.TAG_LONG_ARRAY)) {
            data.changedSections.addAll(LongArrayList.wrap(
                    nbt.getLongArray("changedSections")
            ));
        }

        if (nbt.contains("dirty", Tag.TAG_LONG_ARRAY)) {
            data.dirty.addAll(LongArrayList.wrap(
                    nbt.getLongArray("dirty")
            ));
        }

        if (nbt.contains("loadedSections", Tag.TAG_LONG_ARRAY)) {
            data.loadedSections.addAll(LongArrayList.wrap(
                    nbt.getLongArray("loadedSections")
            ));
        }

        return data;
    }

    private <T extends AbstractDataLayer> void registerLayer(DataLayerType type) {
        layers.put(type, new Long2ObjectOpenHashMap<>());
    }

    private static EnumMap<DataLayerType, Long2ObjectMap<AbstractDataLayer>> deserializeLayers(CompoundTag nbt) {
        EnumMap<DataLayerType, Long2ObjectMap<AbstractDataLayer>> layers = new EnumMap<>(DataLayerType.class);

        for (DataLayerType type : DataLayerType.values()) {

            if (!nbt.contains(type.id, Tag.TAG_COMPOUND)) continue;

            CompoundTag                       layerTag = nbt.getCompound(type.id);
            Long2ObjectMap<AbstractDataLayer> map      = new Long2ObjectOpenHashMap<>();

            for (String keyLong : layerTag.getAllKeys()) {
                long   sectionPos = Long.parseLong(keyLong);
                byte[] bytes      = layerTag.getByteArray(keyLong);

                AbstractDataLayer layer = type.createLayer().fromBytes(bytes);
                map.put(sectionPos, layer);
            }

            layers.put(type, map);
        }

        return layers;
    }

    private static Long2ObjectMap<DataLayerType[]> deserializeInit(
            CompoundTag nbt) {
        Long2ObjectMap<DataLayerType[]> toInit = new Long2ObjectOpenHashMap<>();

        for (String key : nbt.getAllKeys()) {
            long    sectionPos = Long.parseLong(key);
            ListTag list       = nbt.getList(key, Tag.TAG_STRING);

            List<DataLayerType> types = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                String        id   = list.getString(i);
                DataLayerType type = DataLayerType.REGISTRY.get(id);
                if (type != null) {
                    types.add(type);
                }
            }

            toInit.put(sectionPos, types.toArray(DataLayerType[]::new));
        }

        return toInit;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("DataLayerVersion", DATA_VERSION);
        nbt.put("layers", serializeLayers(layers));
        nbt.putLongArray("changedBlocks", changedBlocks.stream().mapToLong(BlockPos::asLong).toArray());
        nbt.put("toInitialise", serializeInit(toInitialise));
        nbt.putLongArray("changedSections", changedSections.toLongArray());
        nbt.putLongArray("dirty", dirty.toLongArray());
        nbt.putLongArray("loadedSections", loadedSections.toLongArray());
        return nbt;
    }

    //
    private static CompoundTag serializeLayers(Map<DataLayerType, Long2ObjectMap<AbstractDataLayer>> layers) {
        CompoundTag nbt = new CompoundTag();

        for (Map.Entry<DataLayerType, Long2ObjectMap<AbstractDataLayer>> entry : layers.entrySet()) {
            DataLayerType                     layerType = entry.getKey();
            Long2ObjectMap<AbstractDataLayer> map       = entry.getValue();

            CompoundTag acc = new CompoundTag();
            for (Long2ObjectMap.Entry<AbstractDataLayer> e : map.long2ObjectEntrySet()) {
                acc.putByteArray(
                        Long.toString(e.getLongKey()),
                        e.getValue().toBytes()
                );
            }

            nbt.put(layerType.id, acc);
        }

        return nbt;
    }

    private static CompoundTag serializeInit(Long2ObjectMap<DataLayerType[]> toInit) {
        CompoundTag nbt = new CompoundTag();

        for (Long2ObjectMap.Entry<DataLayerType[]> entry : toInit.long2ObjectEntrySet()) {
            ListTag list = new ListTag();
            for (DataLayerType layerType : entry.getValue()) {
                list.add(StringTag.valueOf(layerType.id));
            }
            nbt.put(String.valueOf(entry.getLongKey()), list);
        }

        return nbt;
    }

    // ------------------------------
    //  GENERIC ACCESSORS
    // ------------------------------

    public void putLayer(long section, DataLayerType type, AbstractDataLayer dataLayer) {
        layers.get(type).put(section, dataLayer);
        loadedSections.add(section);
    }

    public AbstractDataLayer[] getLayers(long section, DataLayerType... types) {
        AbstractDataLayer[] result = new AbstractDataLayer[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = getLayer(section, types[i]);
        }
        return result;
    }

    public @Nullable AbstractDataLayer getLayer(long section, DataLayerType type) {
        Long2ObjectMap<AbstractDataLayer> map = layers.get(type);
        if (map == null) return null;
        return map.get(section);
    }

    // ------------------------------
    //  INITIALIZATION / PUT
    // ------------------------------

    public LongSet getNearDynamic() {
        return nearDynamicSections; // You can safely expose this if you're not modifying it
    }

    // ------------------------------
    //  SECTION INITIALIZATION
    // ---------------

    public LongSet getLoadedSections() {
        return loadedSections;
    }

    // ------------------------------
    //  SECTION INITIALIZATION
    // ------------------------------
    public void initialise(ServerLevel level) {
        long startTime = System.nanoTime(); // More accurate timing
        int  processed = 0;

        // Use an iterator so we can safely remove elements while iterating
        LongIterator             iterator   = toInitialise.keySet().iterator();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        while (iterator.hasNext() && processed < 10000) {
            if ((System.nanoTime() - startTime) > 20_000_000L) { // 20 ms
                CROWNS.LOGGER.warn("Exiting initialisation for this tick with {} more Sections to go, it took {}ms", toInitialise.size(), (System.nanoTime() - startTime)/1_000_000);
                break;
            }

            long            sectionLong  = iterator.nextLong();
            DataLayerType[] layersToInit = toInitialise.get(sectionLong);

            SectionPos sectionPos = SectionPos.of(sectionLong);
            BlockPos   base       = sectionPos.origin();

            // Skip section if not loaded or not near dynamic blocks, but don't remove it from set
            if (level.isOutsideBuildHeight(base)){
                iterator.remove();
                //toInitialise.remove(sectionLong);
                continue;
            }

            if (!level.isLoaded(base)) {
                //iterator.remove();
                continue;
            }

            if (!nearDynamicSections.contains(sectionLong)) {
                iterator.remove();
                continue;
            }

            //Remove from set once we are processing it
            //toInitialise.remove(sectionLong);
            iterator.remove();//it seems that this doesn't remove it from the toInitialise longMap

            boolean canBeDirty = false;
            float   lastTemp   = -1;

            // Abstracted layer initialization
            for (DataLayerType type : layersToInit) {
                AbstractDataLayer layer   = type.createLayer();
                LevelChunk        chunk   = level.getChunk(sectionPos.x(), sectionPos.z());
                LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionPos.y()));
                for (short i = 0; i < 4096; i++) {
                    int dx = i & 15;
                    int dy = (i >> 8) & 15;
                    int dz = (i >> 4) & 15;

                    mutablePos.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState blockState = section.getBlockState(dx, dy, dz);
                    float      value      = type.getInitializer().apply(section, mutablePos, blockState);
                    layer.setDirect(i, value);

                    if (dynamicContains(mutablePos.asLong())) canBeDirty = true;//if there is a dynamic block it's forced to be dirty

                    // Only track temperature changes for dirty check
                    if (type == DataLayerType.TEMPERATURE) {
                        if (lastTemp != -1 && lastTemp != value) canBeDirty = true;
                        lastTemp = value;
                    }
                }

                layers.get(type).put(sectionLong, layer);
            }

            loadedSections.add(sectionLong);

            // Mark section clean if possible
            if (canBeDirty) {
                setNeedTicking(sectionLong);
            } else {
                setNoTicking(sectionLong);
            }

            processed++;
        }
    }

    public void setNoTicking(long sectionPos) {
        dirty.remove(sectionPos);
    }

    public void updateChangedBlocks(ServerLevel level) {
        long startTime = System.nanoTime();

        DataLayerType[] types = {
                DataLayerType.DEFAULT_TEMPERATURE,
                DataLayerType.CONDUCTION,
                DataLayerType.RESILIENCE
        };

        // Group changed blocks by section
        Long2ObjectMap<List<BlockPos>> bySection = new Long2ObjectOpenHashMap<>();

        int processed = 0;

        while (processed < 10_000 && !changedBlocks.isEmpty()) {
            if (System.nanoTime() - startTime > 20_000_000L) {
                return;
            }

            BlockPos pos = changedBlocks.poll();
            if (pos == null) break;

            long section = SectionPos.asLong(
                    pos.getX() >> 4,
                    pos.getY() >> 4,
                    pos.getZ() >> 4
            );

            bySection
                    .computeIfAbsent(section, k -> new ArrayList<>())
                    .add(pos);

            processed++;
        }

        // Process one section at a time
        for (Long2ObjectMap.Entry<List<BlockPos>> entry : bySection.long2ObjectEntrySet()) {

            if (System.nanoTime() - startTime > 20_000_000L) {
                return;
            }

            SectionPos sectionPos = SectionPos.of(entry.getLongKey());
            LevelChunk chunk = level.getChunk(sectionPos.x(), sectionPos.z());
            int sectionIndex = chunk.getSectionIndexFromSectionY(sectionPos.y());

            if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
                continue;
            }

            LevelChunkSection section = chunk.getSection(sectionIndex);

            for (BlockPos pos : entry.getValue()) {

                if (System.nanoTime() - startTime > 20_000_000L) {
                    return;
                }

                int x = pos.getX() & 15;
                int y = pos.getY() & 15;
                int z = pos.getZ() & 15;

                BlockState state = section.getBlockState(x, y, z);

                set(
                        pos,
                        types,
                        types[0].getInitializer().apply(section, pos, state),
                        types[1].getInitializer().apply(section, pos, state),
                        types[2].getInitializer().apply(section, pos, state)
                );

                setNeedTicking(entry.getLongKey());
            }
        }
    }

    public void set(BlockPos pos, DataLayerType[] types, float... values) {
        if (types.length != values.length) {
            throw new IllegalArgumentException("Types and values arrays must have the same length");
        }

        // --- Compute packed section coordinates ---
        int  sx            = pos.getX() >> 4;
        int  sy            = pos.getY() >> 4;
        int  sz            = pos.getZ() >> 4;
        long packedSection = SectionPos.asLong(sx, sy, sz);

        // --- Local coordinates inside the section ---
        int lx = pos.getX() & 15;
        int ly = pos.getY() & 15;
        int lz = pos.getZ() & 15;

        // --- Set values dynamically ---
        for (int i = 0; i < types.length; i++) {
            AbstractDataLayer layer = getLayer(packedSection, types[i]);
            if (layer != null) {
                layer.set((short) lx, (short) ly, (short) lz, values[i]);
            }
            setNeedTicking(packedSection);
        }
    }

    //to avoid ticking stable sections.
    public void setNeedTicking(long sectionPos) {
        dirty.add(sectionPos);
        //changedSections.add(sectionPos);
    }

    //IHaveTemperature management
    public void putDynamic(BlockPos pos, IHaveTemperature dynamic) {
        //System.out.println("setting dynamic data at "+ pos);
        dynamicData.put(pos.asLong(), dynamic);
        DataLayerType[] layerTypes = {
                DataLayerType.TEMPERATURE,
                DataLayerType.DEFAULT_TEMPERATURE,
                DataLayerType.CONDUCTION,
                DataLayerType.RESILIENCE
        };

        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;

        for (int dx = -DYNAMIC_RANGE; dx <= DYNAMIC_RANGE; dx++) {
            int nsx = sx + dx;
            for (int dy = -DYNAMIC_RANGE; dy <= DYNAMIC_RANGE; dy++) {
                int nsy = sy + dy;
                for (int dz = -DYNAMIC_RANGE; dz <= DYNAMIC_RANGE; dz++) {
                    int  nsz    = sz + dz;
                    long packed = SectionPos.asLong(nsx, nsy, nsz);
                    nearDynamicSections.add(packed);
                    sectionDynamicCount.put(packed, sectionDynamicCount.getOrDefault(packed, 0) + 1);
                    if (loadedSections.contains(packed)) {
                        List<DataLayerType> missingLayers = new ArrayList<>();

                        for (DataLayerType type : layerTypes) {
                            if (!layers.get(type).containsKey(packed)) {
                                missingLayers.add(type);
                            }
                        }

                        if (!missingLayers.isEmpty()) {
                            // Schedule only missing layers
                            scheduleInitialisation(packed, missingLayers.toArray(new DataLayerType[0]));
                            //System.out.printf("resting the section for %s\n", missingLayers);

                        }
                    } else if (!toInitialise.containsKey(packed)) {
                        scheduleInitialisation(packed, layerTypes);
                        //System.out.print("resting the section\n");
                    }
                }
            }
        }
    }

    public void scheduleInitialisation(long section, DataLayerType... layers) {
        // Already scheduled? Just merge missing layers
        if (toInitialise.containsKey(section)) {
            DataLayerType[] existing = toInitialise.get(section);

            // Merge existing layers with new ones, avoiding duplicates
            Set<DataLayerType> merged = new LinkedHashSet<>(Arrays.asList(existing));
            merged.addAll(Arrays.asList(layers));
            toInitialise.put(section, merged.toArray(new DataLayerType[0]));
        } else {
            toInitialise.put(section, layers);
        }

        loadedSections.remove(section);
        setNeedTicking(section);
    }

    public void removeDynamic(BlockPos pos) {
        dynamicData.remove(pos.asLong());

        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;

        for (int dx = -DYNAMIC_RANGE; dx <= DYNAMIC_RANGE; dx++) {
            int nsx = sx + dx;
            for (int dy = -DYNAMIC_RANGE; dy <= DYNAMIC_RANGE; dy++) {
                int nsy = sy + dy;
                for (int dz = -DYNAMIC_RANGE; dz <= DYNAMIC_RANGE; dz++) {
                    int nsz = sz + dz;

                    long packed = SectionPos.asLong(nsx, nsy, nsz);
                    int  count  = sectionDynamicCount.getOrDefault(packed, 0) - 1;
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

    public void reinitializeAll() {
        layers.values().forEach(Long2ObjectMap::clear);
        cachedMatrices.clear();
        dirty.clear();

        //we need an
        for (long section : loadedSections.stream().toList()) {
            scheduleInitialisation(section,
                    DataLayerType.TEMPERATURE,
                    DataLayerType.DEFAULT_TEMPERATURE,
                    DataLayerType.CONDUCTION,
                    DataLayerType.RESILIENCE
            );
        }
    }

    public Long2ObjectMap<IHaveTemperature> getDynamicData() {
        return dynamicData;
    }

    public boolean dynamicContains(long pos) {
        return dynamicData.containsKey(pos);
    }

    public IHaveTemperature getDynamic(long pos) {
        return dynamicData.get(pos);
    }


    //Dirty sections are section that need to be ticked, not sections that need to be rebuilt
    public boolean needTicking(long sectionPos) {
        return dirty.contains(sectionPos);
    }

    public void setCurrentTime(int time) {
        this.currentTime = time;
    }

    public void registerChanged(BlockPos immutable) {
        if (loadedSections.contains(SectionPos.of(immutable).asLong())) {
            changedBlocks.add(immutable);
        }
    }

    public void syncWithPlayers(List<ServerPlayer> players) {
        if (players.isEmpty()) return;

        final int batchSize = 10;

        // --- Get changed sections ---
        List<Long> changed = new ArrayList<>(changedSections);
        if (changed.isEmpty()) return;

        // --- Data maps ---
        var tempMap = layers.get(DataLayerType.TEMPERATURE);//todo we should have a synced boolean on the DataLayerType enum

        for (int i = 0; i < changed.size(); i += batchSize) {
            int        end   = Math.min(i + batchSize, changed.size());
            List<Long> batch = changed.subList(i, end);

            Map<SectionPos, TemperatureDataLayer> tBatch = new HashMap<>();

            for (long section : batch) {
                SectionPos pos = SectionPos.of(section);

                TemperatureDataLayer t = (TemperatureDataLayer) tempMap.get(section);

                if (t != null) tBatch.put(pos, t);
            }

            if (tBatch.isEmpty()) continue;

            UpdateSectionsPacket packet = new UpdateSectionsPacket(tBatch);

            // Send to all players (could be filtered by proximity if desired)
            for (ServerPlayer player : players) {

                PacketDistributor.sendToPlayer(player, packet);
            }

            // Remove sent sections from dirty set
            batch.forEach((s) -> changedSections.remove((long) s));
        }
    }

    public boolean ticked(long sectionPos, int tick) {
        return tickedSections.getOrDefault(sectionPos, -1) >= tick - 1; // ticked this tick or the previous one
    }

    public void addToTicked(long sectionPos) {
        tickedSections.put(sectionPos, currentTime);
        changedSections.add(sectionPos);
    }

    public void resetTicked() {
        tickedSections.clear();
    }

    public boolean checkValidity(long sectionPos) {
        AbstractDataLayer temperatureData        = getLayer(sectionPos, DataLayerType.TEMPERATURE);
        AbstractDataLayer defaultTemperatureData = getLayer(sectionPos, DataLayerType.DEFAULT_TEMPERATURE);
        AbstractDataLayer conductionData         = getLayer(sectionPos, DataLayerType.CONDUCTION);
        AbstractDataLayer resilienceData         = getLayer(sectionPos, DataLayerType.RESILIENCE);

        boolean corrupted = false;

        if (temperatureData == null) {
            CROWNS.LOGGER.warn("error trying to load temperature data at {}", SectionPos.of(sectionPos));
            scheduleInitialisation(sectionPos, DataLayerType.TEMPERATURE); //data got corrupted.
            corrupted = true;
        }
        if (defaultTemperatureData == null) {
            CROWNS.LOGGER.warn("error trying to load default temperature data at {}", SectionPos.of(sectionPos));
            scheduleInitialisation(sectionPos, DataLayerType.DEFAULT_TEMPERATURE); //data got corrupted.
            corrupted = true;
        }
        if (conductionData == null) {
            CROWNS.LOGGER.warn("error trying to load conduction data at {}", SectionPos.of(sectionPos));
            scheduleInitialisation(sectionPos, DataLayerType.CONDUCTION); //data got corrupted.
            corrupted = true;
        }
        if (resilienceData == null) {
            CROWNS.LOGGER.warn("error trying to load resilience data at {}", SectionPos.of(sectionPos));
            scheduleInitialisation(sectionPos, DataLayerType.RESILIENCE); //data got corrupted.
            corrupted = true;
        }
        return !corrupted;
    }

    public Map<DataLayerType, List<Long>> remainingInitialise() {
        HashMap<DataLayerType, List<Long>> collector = new HashMap<>();
        toInitialise.forEach((sectionPos, dataLayerType) -> {
            for (DataLayerType layerType : dataLayerType) {
                List<Long> list = collector.getOrDefault(layerType, new ArrayList<>());
                list.add(sectionPos);
                collector.put(layerType, list);

            }
        });
        return collector;
    }

    public @Nullable AbstractMatrixPhysicsSolver.PhysicsMatrix getCachedMatrix(AbstractMatrixPhysicsSolver<?> solver) {
        return cachedMatrices.get(solver);
    }

    public void setCachedMatrix(AbstractMatrixPhysicsSolver<?> solver, AbstractMatrixPhysicsSolver.PhysicsMatrix newMatrix) {
        this.cachedMatrices.put(solver, newMatrix);
    }
}