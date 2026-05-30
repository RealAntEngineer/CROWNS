package com.rae.crowns.content.fields.util;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.ConductionDataLayer;
import com.rae.crowns.content.fields.temperature.ResilienceDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NonnullDefault;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.rae.crowns.content.fields.util.PosPackingUtil.packSection;

@NonnullDefault
public class PhysicsWorldData extends SavedData {//Only for the server

    //in the future hook into ChunkSection directly : easier for communication and initialization

    public static final  int                                                      DATA_VERSION        = 13;
    private static final int                                                      DYNAMIC_RANGE       = 1;
    // Generic unified map: one Long2ObjectMap per DataLayerType
    private final        Map<DataLayerType<?>, Long2ObjectMap<AbstractDataLayer>> layers              = new HashMap<>();//stored
    // Dynamic and meta state
    private final        Long2ObjectMap<DataLayerType<?>[]>                       toInitialise        = new Long2ObjectOpenHashMap<>();//stored
    private final        Queue<BlockPos>                                          changedBlocks       = new ConcurrentLinkedQueue<>();//stored
    private final        LongSet                                                  changedSections     = new LongOpenHashSet();//stored
    private final        LongSet                                                  dirty               = new LongOpenHashSet();//stored
    private final        LongSet                                                  loadedSections      = new LongOpenHashSet();//stored
    private final        Long2IntMap                                              tickedSections      = new Long2IntOpenHashMap();//recomputed
    private final        Long2ObjectMap<IHaveTemperature>                         dynamicData         = new Long2ObjectOpenHashMap<>();//recomputed
    private final        Long2IntMap                                              sectionDynamicCount = new Long2IntOpenHashMap();//recomputed
    private final        LongSet                                                  nearDynamicSections = new LongOpenHashSet();//recomputed
    private              int                                                      currentTime         = -1;//recomputed


    //matrix

    private final HashMap<AbstractMatrixPhysicsSolver, AbstractMatrixPhysicsSolver.PhysicsMatrix> cachedMatrices = new HashMap<>();

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

    private <T extends AbstractDataLayer> void registerLayer(DataLayerType<T> type) {
        layers.put(type, new Long2ObjectOpenHashMap<>());
    }

    private static Map<DataLayerType<?>, Long2ObjectMap<AbstractDataLayer>> deserializeLayers(CompoundTag nbt) {
        Map<DataLayerType<?>, Long2ObjectMap<AbstractDataLayer>> layers = new HashMap<>();

        for (Map.Entry<String, DataLayerType<?>> regEntry : DataLayerType.REGISTRY.entrySet()) {
            String           id   = regEntry.getKey();
            DataLayerType<?> type = regEntry.getValue();

            if (!nbt.contains(id, Tag.TAG_COMPOUND)) continue;

            CompoundTag                       layerTag = nbt.getCompound(id);
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

    private static Long2ObjectMap<DataLayerType<?>[]> deserializeInit(
            CompoundTag nbt) {
        Long2ObjectMap<DataLayerType<?>[]> toInit = new Long2ObjectOpenHashMap<>();

        for (String key : nbt.getAllKeys()) {
            long    sectionPos = Long.parseLong(key);
            ListTag list       = nbt.getList(key, Tag.TAG_STRING);

            List<DataLayerType<?>> types = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                String           id   = list.getString(i);
                DataLayerType<?> type = DataLayerType.REGISTRY.get(id);
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
    private static CompoundTag serializeLayers(Map<DataLayerType<?>, Long2ObjectMap<AbstractDataLayer>> layers) {
        CompoundTag nbt = new CompoundTag();

        for (Map.Entry<DataLayerType<?>, Long2ObjectMap<AbstractDataLayer>> entry : layers.entrySet()) {
            DataLayerType<?>                  layerType = entry.getKey();
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

    private static CompoundTag serializeInit(Long2ObjectMap<DataLayerType<?>[]> toInit) {
        CompoundTag nbt = new CompoundTag();

        for (Long2ObjectMap.Entry<DataLayerType<?>[]> entry : toInit.long2ObjectEntrySet()) {
            ListTag list = new ListTag();
            for (DataLayerType<?> layerType : entry.getValue()) {
                list.add(StringTag.valueOf(layerType.id));
            }
            nbt.put(String.valueOf(entry.getLongKey()), list);
        }

        return nbt;
    }

    // ------------------------------
    //  GENERIC ACCESSORS
    // ------------------------------

    @SuppressWarnings("unchecked")
    public <T extends AbstractDataLayer> void putLayer(long section, DataLayerType<?> type, T dataLayer) {
        ((Long2ObjectMap<T>) layers.get(type)).put(section, dataLayer);
        loadedSections.add(section);
    }

    public AbstractDataLayer[] getLayers(long section, DataLayerType<?>... types) {
        AbstractDataLayer[] result = new AbstractDataLayer[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = getLayer(section, types[i]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractDataLayer> @Nullable T getLayer(long section, DataLayerType<T> type) {
        Long2ObjectMap<AbstractDataLayer> map = layers.get(type);
        if (map == null) return null;
        return (T) map.get(section);
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
                CROWNS.LOGGER.warn("Exiting initialisation for this tick with {} more Sections to go", toInitialise.size());
                break;
            }

            long               sectionLong  = iterator.nextLong();
            DataLayerType<?>[] layersToInit = toInitialise.get(sectionLong);

            SectionPos sectionPos = SectionPos.of(sectionLong);
            BlockPos   base       = sectionPos.origin();

            // Skip section if not loaded or not near dynamic blocks, but don't remove it from set
            if (!level.isLoaded(base)) {
                //iterator.remove();
                continue;
            }
            if (!nearDynamicSections.contains(sectionLong)) {
                iterator.remove();
                continue;
            }

            // ✅ Remove from set once we are processing it
            //toInitialise.remove(sectionLong);
            iterator.remove();//it seems that this doesn't remove it from the toInitialise longMap

            boolean canBeDirty = false;
            float   lastTemp   = -1;

            // Abstracted layer initialization
            for (DataLayerType<?> type : layersToInit) {
                AbstractDataLayer layer = type.createLayer();

                for (int i = 0; i < 4096; i++) {
                    int dx = i & 15;
                    int dy = (i >> 4) & 15;
                    int dz = (i >> 8) & 15;

                    mutablePos.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState blockState = level.getBlockState(mutablePos);
                    float      value      = type.getInitializer().apply(level, mutablePos, blockState);
                    layer.set(dx, dy, dz, value);

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
            if (!canBeDirty && !nearDynamicSections.contains(sectionLong)) {
                setClean(sectionLong);
            }

            processed++;
        }
    }

    public void setClean(long sectionPos) {
        dirty.remove(sectionPos);
    }

    public void updateChangedBlocks(ServerLevel level) {
        float              initialTimeMS = System.currentTimeMillis();
        DataLayerType<?>[] types         = {DataLayerType.DEFAULT_TEMPERATURE, DataLayerType.CONDUCTION, DataLayerType.RESILIENCE};

        for (int i = 0; i < 10000 && !changedBlocks.isEmpty(); i++) {
            BlockPos   pos   = changedBlocks.poll();
            BlockState state = level.getBlockState(pos);

            set(pos, types, PhysicsSaveManager.getDefaultTemperature(level, pos, state),
                    PhysicsSaveManager.getDefaultConduction(state), PhysicsSaveManager.getDefaultResilience(state));

            // --- Update solid mask using PassThroughTester ---
            //updateBlockedFaces(level, pos, state);

            setDirty(SectionPos.of(pos).asLong());
            if (System.currentTimeMillis() - initialTimeMS > 20) {
                break;
            }
        }
    }

    public void set(BlockPos pos, DataLayerType<?>[] types, float... values) {
        if (types.length != values.length) {
            throw new IllegalArgumentException("Types and values arrays must have the same length");
        }

        // --- Compute packed section coordinates ---
        int  sx            = pos.getX() >> 4;
        int  sy            = pos.getY() >> 4;
        int  sz            = pos.getZ() >> 4;
        long packedSection = packSection(sx, sy, sz);

        // --- Local coordinates inside the section ---
        int lx = pos.getX() & 15;
        int ly = pos.getY() & 15;
        int lz = pos.getZ() & 15;

        // --- Set values dynamically ---
        for (int i = 0; i < types.length; i++) {
            AbstractDataLayer layer = getLayer(packedSection, types[i]);
            if (layer != null) {
                layer.set(lx, ly, lz, values[i]);
            }
        }
    }

    //to avoid ticking stable sections.
    public void setDirty(long sectionPos) {
        dirty.add(sectionPos);
        changedSections.add(sectionPos);
    }

    //IHaveTemperature management
    public void putDynamic(BlockPos pos, IHaveTemperature dynamic) {
        //System.out.println("setting dynamic data at "+ pos);
        dynamicData.put(pos.asLong(), dynamic);
        DataLayerType<?>[] layerTypes = {
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
                    int nsz = sz + dz;

                    if (isInDynamicRange(pos, nsx, nsy, nsz)) {
                        long packed = packSection(nsx, nsy, nsz);
                        nearDynamicSections.add(packed);
                        sectionDynamicCount.put(packed, sectionDynamicCount.getOrDefault(packed, 0) + 1);
                        if (loadedSections.contains(packed)) {
                            List<DataLayerType<?>> missingLayers = new ArrayList<>();

                            for (DataLayerType<?> type : layerTypes) {
                                if (!layers.get(type).containsKey(packed)) {
                                    missingLayers.add(type);
                                }
                            }

                            if (!missingLayers.isEmpty()) {
                                // Schedule only missing layers
                                scheduleInitialisation(packed, missingLayers.toArray(new DataLayerType<?>[0]));
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

    public void scheduleInitialisation(long section, DataLayerType<?>... layers) {
        // Already scheduled? Just merge missing layers
        if (toInitialise.containsKey(section)) {
            DataLayerType<?>[] existing = toInitialise.get(section);

            // Merge existing layers with new ones, avoiding duplicates
            Set<DataLayerType<?>> merged = new LinkedHashSet<>(Arrays.asList(existing));
            merged.addAll(Arrays.asList(layers));
            toInitialise.put(section, merged.toArray(new DataLayerType<?>[0]));
        } else {
            toInitialise.put(section, layers);
        }

        loadedSections.remove(section);
        setDirty(section);
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

                    if (isInDynamicRange(pos, nsx, nsy, nsz)) {
                        long packed = packSection(nsx, nsy, nsz);
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

    public boolean isDirty(long sectionPos) {
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
            batch.forEach(changedSections::remove);
        }
    }

    public boolean ticked(long sectionPos, int tick) {
        return tickedSections.getOrDefault(sectionPos, -1) < tick + 1;//small acceptable delay
    }

    public void addToTicked(long sectionPos) {
        tickedSections.put(sectionPos, currentTime);
    }

    public void resetTicked() {
        tickedSections.clear();
    }

    public boolean checkValidity(long sectionPos) {
        TemperatureDataLayer temperatureData        = getLayer(sectionPos, DataLayerType.TEMPERATURE);
        TemperatureDataLayer defaultTemperatureData = getLayer(sectionPos, DataLayerType.DEFAULT_TEMPERATURE);
        ConductionDataLayer  conductionData         = getLayer(sectionPos, DataLayerType.CONDUCTION);
        ResilienceDataLayer  resilienceData         = getLayer(sectionPos, DataLayerType.RESILIENCE);

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

    public Map<DataLayerType<?>, List<Long>> remainingInitialise() {
        HashMap<DataLayerType<?>, List<Long>> collector = new HashMap<>();
        toInitialise.forEach((sectionPos, dataLayerType) -> {
            for (DataLayerType<?> layerType : dataLayerType) {
                List<Long> list = collector.getOrDefault(layerType, new ArrayList<>());
                list.add(sectionPos);
                collector.put(layerType, list);

            }
        });
        return collector;
    }

    public AbstractMatrixPhysicsSolver.PhysicsMatrix getCachedMatrix(AbstractMatrixPhysicsSolver solver) {
        return cachedMatrices.get(solver);
    }

    public void setCachedMatrix(AbstractMatrixPhysicsSolver solver, AbstractMatrixPhysicsSolver.PhysicsMatrix newMatrix) {
        this.cachedMatrices.put(solver, newMatrix);
    }


    private static class DynamicDataManagement {

    }
}