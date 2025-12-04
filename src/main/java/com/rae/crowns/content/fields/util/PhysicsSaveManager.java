package com.rae.crowns.content.fields.util;

import com.rae.crowns.CROWNS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public class PhysicsSaveManager {
    private static final Map<ResourceKey<Level>, PhysicsWorldData> worldDataMap = new WeakHashMap<>();

    public static @NotNull PhysicsWorldData get(@NotNull ServerLevel level) {
        return worldDataMap.computeIfAbsent(level.dimension(), k -> new PhysicsWorldData());
    }

    public static void reset(){
        worldDataMap.clear();
    }


    /**
     * lock safe version.
     *
     * @param level      the level, doesn't make sens for non server level
     * @param pos        position
     * @param blockState block state at said position
     * @return the default temperature at the position.
     */
    public static float getDefaultTemperature(@NotNull Level level, @NotNull Vec3i pos, @NotNull BlockState blockState) {
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


    public static void sendUpdate(@NotNull ServerLevel level) {
        get(level).syncWithPlayers(level.getPlayers(serverPlayer -> serverPlayer.level().dimension().equals(level.dimension())));
    }
}