package com.rae.crowns.content.fields.util;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.advection.BlockedDataLayer;
import com.rae.crowns.content.fields.advection.VelocityDataLayer;
import com.rae.crowns.content.fields.temperature.*;
import com.rae.crowns.content.thermodynamics.IHaveTemperature;
import com.rae.crowns.init.data.PacketInit;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static com.rae.crowns.content.fields.util.PosPackingUtil.packSection;

public class PhysicsWorldData {//Only for the server

    //in the future hook into ChunkSection directly : easier for communication and initialisation

    private static final int DYNAMIC_RANGE = 2;
    public static final int DATA_VERSION = 12;

    // Generic unified map: one Long2ObjectMap per DataLayerType
    private final Map<DataLayerType<?>, Long2ObjectMap<AbstractDataLayer>> layers = new HashMap<>();

    // Dynamic and meta state
    private final Long2ObjectMap<IHaveTemperature> dynamicData = new Long2ObjectOpenHashMap<>();
    private final LongSet toInitialise = new LongOpenHashSet();
    private final Queue<BlockPos> changedBlocks = new ConcurrentLinkedQueue<>();
    private final LongSet changedSections = new LongOpenHashSet();
    private final LongSet dirty = new LongOpenHashSet();
    private final LongSet loadedSections = new LongOpenHashSet();
    private final Long2IntMap sectionDynamicCount = new Long2IntOpenHashMap();
    private final LongSet nearDynamicSections = new LongOpenHashSet();

    public PhysicsWorldData() {
        // Register default layer maps — any future DataLayerType will also work
        registerLayer(DataLayerType.TEMPERATURE);
        registerLayer(DataLayerType.DEFAULT_TEMPERATURE);
        registerLayer(DataLayerType.CONDUCTION);
        registerLayer(DataLayerType.RESILIENCE);
        registerLayer(DataLayerType.VX);
        registerLayer(DataLayerType.BLOCKED_X);
        registerLayer(DataLayerType.VY);
        registerLayer(DataLayerType.BLOCKED_Y);
        registerLayer(DataLayerType.VZ);
        registerLayer(DataLayerType.BLOCKED_Z);
    }

    private <T extends AbstractDataLayer> void registerLayer(DataLayerType<T> type) {
        layers.put(type, new Long2ObjectOpenHashMap<>());
    }

    // ------------------------------
    //  GENERIC ACCESSORS
    // ------------------------------

    @SuppressWarnings("unchecked")
    public <T extends AbstractDataLayer> T getLayer(DataLayerType<T> type, long section) {
        Long2ObjectMap<AbstractDataLayer> map = layers.get(type);
        if (map == null) return null;
        return (T) map.get(section);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractDataLayer> void putLayer(DataLayerType<?> type, long section, T dataLayer) {
        ((Long2ObjectMap<T>) layers.get(type)).put(section, dataLayer);
        loadedSections.add(section);
    }

    public AbstractDataLayer[] getLayers(long section, DataLayerType<?>... types) {
        AbstractDataLayer[] result = new AbstractDataLayer[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = getLayer(types[i], section);
        }
        return result;
    }

    // ------------------------------
    //  EXISTING CONVENIENCE METHODS
    // ------------------------------

    public TemperatureDataLayer getTemperature(long section) {
        return getLayer(DataLayerType.TEMPERATURE, section);
    }

    public TemperatureDataLayer getDefaultTemperature(long section) {
        return getLayer(DataLayerType.DEFAULT_TEMPERATURE, section);
    }

    public ResilienceDataLayer getResilience(long section) {
        return getLayer(DataLayerType.RESILIENCE, section);
    }

    public ConductionDataLayer getConduction(long section) {
        return getLayer(DataLayerType.CONDUCTION, section);
    }

    // ------------------------------
    //  INITIALIZATION / PUT
    // ------------------------------

    public void put(long section, TemperatureDataLayer temp, TemperatureDataLayer defTemp,
                    ConductionDataLayer cond, ResilienceDataLayer res) {
        putLayer(DataLayerType.TEMPERATURE, section, temp);
        putLayer(DataLayerType.DEFAULT_TEMPERATURE, section, defTemp);
        putLayer(DataLayerType.CONDUCTION, section, cond);
        putLayer(DataLayerType.RESILIENCE, section, res);

        loadedSections.add(section);
    }

    public void putForInitialisation(long section) {
        if (toInitialise.contains(section)) return;
        toInitialise.add(section);
        setDirty(section);
    }

    // ------------------------------
    //  SECTION INITIALIZATION
    // ---------------

    public @NotNull LongSet getNearDynamic() {
        return nearDynamicSections; // You can safely expose this if you're not modifying it
    }

    public @NotNull LongSet getLoadedSections(){
        return loadedSections;
    }

    // ------------------------------
    //  SECTION INITIALIZATION
    // ------------------------------

    public void initialise(@NotNull ServerLevel level) {
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

            // ⚠️ Skip this section if not loaded or not near a dynamic block, keep it in the set for later
            if (!level.isLoaded(sectionPos.origin()) || !nearDynamicSections.contains(section)) {
                iterator.remove();
                continue;
            }

            // ✅ Only remove once we're actually processing it
            iterator.remove();

            TemperatureDataLayer temperatureDataLayer = new TemperatureDataLayer();
            TemperatureDataLayer defaultTemperatureDataLayer = new TemperatureDataLayer();

            ConductionDataLayer conductionDataLayer = new ConductionDataLayer();
            ResilienceDataLayer resilienceDataLayer = new ResilienceDataLayer();

            VelocityDataLayer vx = new VelocityDataLayer();
            BlockedDataLayer blocked_x = new BlockedDataLayer();
            VelocityDataLayer vy = new VelocityDataLayer();
            BlockedDataLayer blocked_y = new BlockedDataLayer();
            VelocityDataLayer vz = new VelocityDataLayer();
            BlockedDataLayer blocked_z = new BlockedDataLayer();

            BlockPos base = sectionPos.origin();
            boolean canBeDirty = false;
            float defaultTemp = -1;

            for (int dx = 0; dx < 16; dx++) {//todo flatten this loop (iterate from 0 to 4095 on a short and do bit manipulation to have the dx,dy,dz)
                for (int dy = 0; dy < 16; dy++) {
                    for (int dz = 0; dz < 16; dz++) {
                        BlockPos pos = base.offset(dx, dy, dz);//todo reduce the usage of object to a minimum.
                        BlockState blockState = level.getBlockState(pos);

                        float oldTemp = defaultTemp;//todo we can probably remove this it's the remanent of an old optimisation trick.
                        defaultTemp = PhysicsSaveManager.getDefaultTemperature(level, pos);

                        temperatureDataLayer.set(dx, dy, dz, defaultTemp);
                        defaultTemperatureDataLayer.set(dx, dy, dz, defaultTemp);
                        conductionDataLayer.set(dx, dy, dz, PhysicsSaveManager.getDefaultConduction(level, pos));
                        resilienceDataLayer.set(dx, dy, dz, PhysicsSaveManager.getDefaultResilience(level, pos));

                        vx.set(dx, dy, dz, 0);
                        vy.set(dx, dy, dz, 0);
                        vz.set(dx, dy, dz, 0);

                        // Always-pass blocks (transparent to airflow)
                        if (PassThroughTester.shouldAlwaysPass(blockState)) {
                            blocked_x.set(dx, dy, dz, 0);
                            blocked_y.set(dx, dy, dz, 0);
                            blocked_z.set(dx, dy, dz, 0);
                        } else if (blockState.isSolid()) {
                            blocked_x.set(dx, dy, dz, 1);
                            blocked_y.set(dx, dy, dz, 1);
                            blocked_z.set(dx, dy, dz, 1);
                        } else {
                            // For each direction, determine if the face is blocked
                            for (Direction dir : Direction.values()) {
                                double depth = PassThroughTester.findMaxDepth(blockState.getCollisionShape(level, pos), dir);
                                boolean blocked = (depth > 0.0 && depth < Double.POSITIVE_INFINITY);
                                switch (dir) {
                                    case EAST, WEST -> blocked_x.set(dx, dy, dz, blocked ? 1 : 0);
                                    case UP, DOWN -> blocked_y.set(dx, dy, dz, blocked ? 1 : 0);
                                    case SOUTH, NORTH -> blocked_z.set(dx, dy, dz, blocked ? 1 : 0);
                                }
                            }
                        }

                        if (oldTemp != -1 && oldTemp != defaultTemp) {
                            canBeDirty = true;
                        }
                    }
                }
            }

            //todo make this abstract (meaning it look at dataLayerType and get the initialise methode there)
            long sectionLong = sectionPos.asLong();
            layers.get(DataLayerType.TEMPERATURE).put(sectionLong, temperatureDataLayer);
            layers.get(DataLayerType.DEFAULT_TEMPERATURE).put(sectionLong, defaultTemperatureDataLayer);
            layers.get(DataLayerType.CONDUCTION).put(sectionLong, conductionDataLayer);
            layers.get(DataLayerType.RESILIENCE).put(sectionLong, resilienceDataLayer);
            layers.get(DataLayerType.VX).put(sectionLong, vx);
            layers.get(DataLayerType.BLOCKED_X).put(sectionLong, blocked_x);
            layers.get(DataLayerType.VY).put(sectionLong, vy);
            layers.get(DataLayerType.BLOCKED_Y).put(sectionLong, blocked_y);
            layers.get(DataLayerType.VZ).put(sectionLong, vz);
            layers.get(DataLayerType.BLOCKED_Z).put(sectionLong, blocked_z);

            loadedSections.add(sectionLong);

            if (!canBeDirty && !nearDynamicSections.contains(sectionLong)) {
                setClean(sectionLong);
            }

            processed++;
        }
    }

    public void updateChangedBlocks(@NotNull ServerLevel level) {
        float initialTimeMS = System.currentTimeMillis();
        for (int i = 0; i < 10000 && !changedBlocks.isEmpty(); i++) {
            BlockPos pos = changedBlocks.poll();
            BlockState state = level.getBlockState(pos);
            set(pos, PhysicsSaveManager.getDefaultTemperature(level, pos, state),
                    PhysicsSaveManager.getDefaultConduction(level, pos), PhysicsSaveManager.getDefaultResilience(level, pos));

            // --- Update solid mask using PassThroughTester ---
            updateBlockedFaces(level, pos, state);

            setDirty(SectionPos.of(pos).asLong());
            if (System.currentTimeMillis() - initialTimeMS > 20) {
                break;
            }
        }
    }

    private void updateBlockedFaces(ServerLevel level, BlockPos pos, BlockState state) {
        long packedSection = packSection(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);

        BlockedDataLayer xPosLayer = getLayer(DataLayerType.BLOCKED_X, packedSection);
        BlockedDataLayer yPosLayer = getLayer(DataLayerType.BLOCKED_Y, packedSection);
        BlockedDataLayer zPosLayer = getLayer(DataLayerType.BLOCKED_Z, packedSection);

        if (xPosLayer == null || yPosLayer == null || zPosLayer == null)
            return;

        int lx = pos.getX() & 15;
        int ly = pos.getY() & 15;
        int lz = pos.getZ() & 15;

        // Always-pass blocks (transparent to airflow)
        if (PassThroughTester.shouldAlwaysPass(state)) {
            xPosLayer.set(lx, ly, lz, 0);
            yPosLayer.set(lx, ly, lz, 0);
            zPosLayer.set(lx, ly, lz, 0);
            return;
        }
        if (state.isSolid()) {
            xPosLayer.set(lx, ly, lz, 1);
            yPosLayer.set(lx, ly, lz, 1);
            zPosLayer.set(lx, ly, lz, 1);
            return;
        }

        // For each direction, determine if the face is blocked
        for (Direction dir : Direction.values()) {
            double depth = PassThroughTester.findMaxDepth(state.getCollisionShape(level, pos), dir);
            boolean blocked = (depth > 0.0 && depth < Double.POSITIVE_INFINITY);
            switch (dir) {
                case EAST, WEST -> xPosLayer.set(lx, ly, lz, blocked ? 1 : 0);
                case UP, DOWN -> yPosLayer.set(lx, ly, lz, blocked ? 1 : 0);
                case SOUTH, NORTH -> zPosLayer.set(lx, ly, lz, blocked ? 1 : 0);
            }
        }
    }

    public void set(@NotNull BlockPos pos, float temperature, float conduction, float resilience) {
        // --- Compute packed section coordinates manually ---
        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;
        long packedSection = packSection(sx, sy, sz);

        // --- Get layers ---
        TemperatureDataLayer temperatureDataLayer = getTemperature(packedSection);
        TemperatureDataLayer defaultTemperatureDataLayer = getDefaultTemperature(packedSection);

        ConductionDataLayer conductionDataLayer = getConduction(packedSection);
        ResilienceDataLayer resilienceDataLayer = getResilience(packedSection);

        if (temperatureDataLayer != null && conductionDataLayer != null && resilienceDataLayer != null) {
            // --- Local coordinates inside the section ---
            int lx = pos.getX() & 15;
            int ly = pos.getY() & 15;
            int lz = pos.getZ() & 15;

            // --- Set values once ---
            temperatureDataLayer.set(lx, ly, lz, temperature);
            defaultTemperatureDataLayer.set(lx, ly, lz, temperature);
            conductionDataLayer.set(lx, ly, lz, conduction);
            resilienceDataLayer.set(lx, ly, lz, resilience);

            // --- No need to put layers back if your map already stores references ---
            // put(packedSection, temperatureDataLayer);
            // put(packedSection, conductionDataLayer);
            // put(packedSection, resilienceDataLayer);
        }
    }

    //IHaveTemperature management

    public void putDynamic(@NotNull BlockPos pos, IHaveTemperature dynamic) {
        dynamicData.put(pos.asLong(), dynamic);

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
                        if (!loadedSections.contains(packed)) {
                            putForInitialisation(packed);
                        }
                    }
                }
            }
        }
    }

    public void removeDynamic(@NotNull BlockPos pos) {
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

    public @NotNull Long2ObjectMap<IHaveTemperature> getDynamicData() {
        return dynamicData;
    }

    public boolean dynamicContains(long pos) {
        return dynamicData.containsKey(pos);
    }

    public IHaveTemperature getDynamic(long pos) {
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

    public void registerChanged(@NotNull BlockPos immutable) {
        if (loadedSections.contains(SectionPos.of(immutable).asLong())) {
            changedBlocks.add(immutable);
        }
    }


    public void syncWithPlayers(@NotNull List<ServerPlayer> players) {
        if (players.isEmpty()) return;

        final int batchSize = 10;

        // --- Get changed sections ---
        List<Long> changed = new ArrayList<>(changedSections);
        if (changed.isEmpty()) return;

        // --- Data maps ---
        var tempMap = layers.get(DataLayerType.TEMPERATURE);//todo we should have a synced boolean on the DataLayerType enum
        var vxMap   = layers.get(DataLayerType.VX);
        var vyMap   = layers.get(DataLayerType.VY);
        var vzMap   = layers.get(DataLayerType.VZ);

        for (int i = 0; i < changed.size(); i += batchSize) {
            int end = Math.min(i + batchSize, changed.size());
            List<Long> batch = changed.subList(i, end);

            Map<SectionPos, TemperatureDataLayer> tBatch = new HashMap<>();
            Map<SectionPos, VelocityDataLayer> vxBatch = new HashMap<>();
            Map<SectionPos, VelocityDataLayer> vyBatch = new HashMap<>();
            Map<SectionPos, VelocityDataLayer> vzBatch = new HashMap<>();

            for (long section : batch) {
                SectionPos pos = SectionPos.of(section);

                TemperatureDataLayer t = (TemperatureDataLayer) tempMap.get(section);
                VelocityDataLayer vxL  = (VelocityDataLayer) vxMap.get(section);
                VelocityDataLayer vyL  = (VelocityDataLayer) vyMap.get(section);
                VelocityDataLayer vzL  = (VelocityDataLayer) vzMap.get(section);

                if (t != null)  tBatch.put(pos, t);
                if (vxL != null) vxBatch.put(pos, vxL);
                if (vyL != null) vyBatch.put(pos, vyL);
                if (vzL != null) vzBatch.put(pos, vzL);
            }

            if (tBatch.isEmpty() && vxBatch.isEmpty() && vyBatch.isEmpty() && vzBatch.isEmpty()) continue;

            UpdateSectionsPacket packet = new UpdateSectionsPacket(tBatch, vxBatch, vyBatch, vzBatch);

            // Send to all players (could be filtered by proximity if desired)
            for (ServerPlayer player : players) {
                PacketInit.getChannel().send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        packet
                );
            }

            // Remove sent sections from dirty set
            batch.forEach(changedSections::remove);
        }
    }

    public void dumpSection(long sectionPos) {
        // Remove the section
        layers.get(DataLayerType.TEMPERATURE).remove(sectionPos);
        layers.get(DataLayerType.DEFAULT_TEMPERATURE).remove(sectionPos);
        layers.get(DataLayerType.CONDUCTION).remove(sectionPos);
        layers.get(DataLayerType.RESILIENCE).remove(sectionPos);
        loadedSections.remove(sectionPos);

        // clean up other related queues/maps
        //changedSections.remove(sectionPos);
        dirty.remove(sectionPos);
    }

    // --- HELPER FOR DYNAMIC RANGE CHECK ---
    private static boolean isInDynamicRange(@NotNull Vec3i pos, int sx, int sy, int sz) {
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

    public boolean isLoaded(long sectionPos) {
        return loadedSections.contains(sectionPos);
    }
}