package com.rae.crowns.content.fields.util;

import com.rae.crowns.CROWNS;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NonnullDefault;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NonnullDefault
@EventBusSubscriber(modid = CROWNS.MODID)
public class PhysicsSaveManager {
    private static final Map<ResourceKey<Level>, PhysicsWorldData> worldDataMap = new ConcurrentHashMap<>();
    private static final    Map<ResourceKey<Level>, LongSet> worldLoadedSections = new ConcurrentHashMap<>();
    private static MinecraftServer                  serverInstance;
    //they are here to count the sections that are loaded or not.

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ChunkAccess chunk = event.getChunk();
            // Dump all sections for this chunk
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;

            for (int sectionY = event.getLevel().getMinSection(); sectionY < event.getLevel().getMaxSection(); sectionY++) {
                SectionPos sectionPos = SectionPos.of(chunkX, sectionY, chunkZ);
                worldLoadedSections.computeIfAbsent(serverLevel.dimension(), k -> new LongOpenHashSet()).remove(sectionPos.asLong());
            }
        }
    }


    //doesn't give use newly generated chunks ?
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ChunkAccess chunk = event.getChunk();
            // Dump all sections for this chunk
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;

            for (int sectionY = event.getLevel().getMinSection(); sectionY < event.getLevel().getMaxSection(); sectionY++) {
                SectionPos sectionPos = SectionPos.of(chunkX, sectionY, chunkZ);
                worldLoadedSections.computeIfAbsent(serverLevel.dimension(), k -> new LongOpenHashSet()).add(sectionPos.asLong());
            }
        }
    }

    public static boolean isLoaded(ResourceKey<Level> level, long section) {
        LongSet set = worldLoadedSections.get(level);
        return set != null && set.contains(section);
    }

    public static @Nullable PhysicsWorldData get(ServerLevel level) {
        return worldDataMap.get(level.dimension());
    }

    public static void reset() {
        worldDataMap.clear();
        worldLoadedSections.clear();
    }

    /**
     * lock safe version.
     *
     * @param level      the level, doesn't make sens for non server level
     * @param pos        position
     * @param blockState block state at said position
     * @return the default temperature at the position.
     */
    public static float getDefaultTemperature(Level level, Vec3i pos, BlockState blockState) {
        FluidState fluid = blockState.getFluidState();
        // Convert to quart coordinates (biome resolution)
        int qx = QuartPos.fromBlock(pos.getX());
        int qy = QuartPos.fromBlock(pos.getY());
        int qz = QuartPos.fromBlock(pos.getZ());

        // Get the biome directly from the noise source
        Holder<Biome> biome = level.getBiomeManager()
                .getNoiseBiomeAtQuart(qx, qy, qz);
        float defaultT = CROWNS.BIOME_TEMPERATURES.getValue(biome.value(), 300f);
        //TODO : A mix bwn the 2 ?
        //Priority: Fluid > Block >  Biome
        if (fluid.isEmpty()) {
            return CROWNS.BLOCK_TEMPERATURES.getValue(blockState.getBlock(), defaultT);
        } else {
            return CROWNS.FLUID_TEMPERATURES.getValue(fluid.getType(), defaultT);

        }
    }

    public static float getDefaultConduction(BlockState blockState) {
        FluidState fluid = blockState.getFluidState();
        // Priority: Fluid > Block
        if (fluid.isEmpty()) {
            return CROWNS.BLOCK_CONDUCTION.getValue(blockState.getBlock(), 100);
        } else {
            return CROWNS.FLUID_CONDUCTION.getValue(fluid.getType(), 100);

        }
    }

    public static float getDefaultResilience(BlockState blockState) {
        FluidState fluid = blockState.getFluidState();
        // Priority: Fluid > Block
        if (fluid.isEmpty()) {
            return CROWNS.BLOCK_RESILIENCE.getValue(blockState.getBlock(), 0f);
        } else {
            return CROWNS.FLUID_RESILIENCE.getValue(fluid.getType(), 0f);

        }
    }

    public static void sendUpdate(ServerLevel level) {
        PhysicsWorldData data = worldDataMap.get(level.dimension());
        if (data == null) return;
        data.syncWithPlayers(level.getPlayers(serverPlayer -> serverPlayer.level().dimension().equals(level.dimension())));
    }

    public static void serverStarted(MinecraftServer server) {
        serverInstance = server;
        for (ServerLevel level : server.getAllLevels()) {
            worldDataMap.put(level.dimension(), PhysicsWorldData.loadData(level));
        }
    }

    public static Iterable<ServerLevel> getServers(){
        if (serverInstance!=null){
            return serverInstance.getAllLevels();
        }
        return List.of();
    }

}