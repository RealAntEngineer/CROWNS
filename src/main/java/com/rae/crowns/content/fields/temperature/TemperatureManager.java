package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.CROWNS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Map;
import java.util.WeakHashMap;

public class TemperatureManager {
    private static final Map<ServerLevel, TemperatureWorldData> worldDataMap = new WeakHashMap<>();

    public static TemperatureWorldData get(ServerLevel level) {
        return worldDataMap.computeIfAbsent(level, k -> new TemperatureWorldData());
    }

    public static float getDefaultTemperature(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        float defaultT = CROWNS.BIOME_TEMPERATURES.getValue(level.getBiome(pos).value(), 300f);
        //TODO : A mix bwn the 2 ?
        //Priority: Fluid > Block >  Biome
        if (fluid.isEmpty()) {
            BlockState blockState = level.getBlockState(pos);
            return CROWNS.BLOCK_TEMPERATURES.getValue(blockState.getBlock(), defaultT);
        } else {
            return CROWNS.FLUID_TEMPERATURES.getValue(fluid.getType(), defaultT);

        }
    }

    /**
     * lock safe version.
     * @param level the level, doesn't make sens for non server level
     * @param pos position
     * @param blockState blockstate at said position
     * @return the default temperature at the position.
     */
    public static float getDefaultTemperature(Level level, BlockPos pos, BlockState blockState) {
        FluidState fluid = blockState.getFluidState();
        float defaultT = CROWNS.BIOME_TEMPERATURES.getValue(level.getBiome(pos).value(), 300f);
        //TODO : A mix bwn the 2 ?
        //Priority: Fluid > Block >  Biome
        if (fluid.isEmpty()) {
            return CROWNS.BLOCK_TEMPERATURES.getValue(blockState.getBlock(), defaultT);
        } else {
            return CROWNS.FLUID_TEMPERATURES.getValue(fluid.getType(), defaultT);

        }
    }

    public static float getDefaultConduction(Level level, Vec3i pos) {
        FluidState fluid = level.getFluidState((BlockPos) pos);
        // Priority: Fluid > Block
        if (!fluid.isEmpty()) {
            return CROWNS.BLOCK_CONDUCTION.getValue(level.getBlockState((BlockPos) pos).getBlock(), 10);
        } else {
            return CROWNS.FLUID_CONDUCTION.getValue(fluid.getType(), 10);

        }
    }
    public static float getDefaultResilience(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        // Priority: Fluid > Block
        if (fluid.isEmpty()) {
            return CROWNS.BLOCK_RESILIENCE.getValue(level.getBlockState(pos).getBlock(), 0.5f);
        } else {
            return CROWNS.FLUID_RESILIENCE.getValue(fluid.getType(), 0.5f);

        }
    }


    public static void sendUpdate(ServerLevel level) {
        get(level).syncWithPlayers(level.getPlayers(serverPlayer -> serverPlayer.level().dimension().equals(level.dimension())));
    }
}
