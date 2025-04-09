package com.rae.crowns.content.fields.temperature;

import com.mojang.serialization.Codec;
import com.rae.crowns.CROWNS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class TemperatureManager {
    private static final Map<ServerLevel, TemperatureWorldData> worldDataMap = new WeakHashMap<>();

    private static final Codec<TemperatureDataLayer> SECTION_CODEC = Codec.BYTE_BUFFER.xmap(
            byteBuffer -> TemperatureDataLayer.fromBytes(byteBuffer.array()),
            temperatureDataLayer -> ByteBuffer.wrap(temperatureDataLayer.toBytes())
    );

    public final static Codec<List<TemperatureDataLayer>> CODEC = Codec.list(SECTION_CODEC);

    public static @NotNull List<TemperatureDataLayer> copyTemperature(List<TemperatureDataLayer> temperatureDataLayers, IAttachmentHolder iAttachmentHolder, HolderLookup.Provider provider) {
        return new ArrayList<>(temperatureDataLayers);
    }

    public static TemperatureWorldData get(ServerLevel level) {
        return worldDataMap.computeIfAbsent(level, k -> new TemperatureWorldData());
    }

    public static float getDefaultTemperature(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        float defaultT = CROWNS.BIOME_TEMPERATURES.getValue(level.getBiome(pos).value(), 300f);
        //TODO : A mix bwn the 2 ?
        //Priority: Fluid > Block >  Biome
        if (fluid.isEmpty()) {
            return CROWNS.BLOCK_TEMPERATURES.getValue(level.getBlockState(pos).getBlock(), defaultT);
        } else {
            return CROWNS.FLUID_TEMPERATURES.getValue(fluid.getType(), defaultT);

        }
    }

    static float getBlockConduction(Level level, Vec3i pos) {
        FluidState fluid = level.getFluidState((BlockPos) pos);
        // Priority: Fluid > Block
        if (!fluid.isEmpty()) {
            return CROWNS.BLOCK_CONDUCTION.getValue(level.getBlockState((BlockPos) pos).getBlock(), 10000);
        } else {
            return CROWNS.FLUID_CONDUCTION.getValue(fluid.getType(), 10000);

        }
    }
    static float getBlockCapacity(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        // Priority: Fluid > Block
        if (fluid.isEmpty()) {
            return CROWNS.BLOCK_CAPACITY.getValue(level.getBlockState(pos).getBlock(), 100000);
        } else {
            return CROWNS.FLUID_CAPACITY.getValue(fluid.getType(), 100000);

        }
    }
}
