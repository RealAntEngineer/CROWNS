package com.rae.crowns.init.data;

import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

// TODO put this in the formic API
public class StateMapSerializer implements EntityDataSerializer<HashMap<BlockPos, SpecificRealGasState>> {
    public StateMapSerializer() {
    }

    @Override
    public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, HashMap<BlockPos, SpecificRealGasState>> codec() {
        return new StreamCodec<>() {
            @Override
            public @NotNull HashMap<BlockPos, SpecificRealGasState> decode(@NotNull RegistryFriendlyByteBuf buffer) {
                HashMap<BlockPos, SpecificRealGasState> stateMap = new HashMap<>();
                int                                     size     = buffer.readInt();
                for (int i = 0; i < size; i++) {
                    stateMap.put(buffer.readBlockPos(), new SpecificRealGasState(buffer.readFloat(), buffer.readFloat(),buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

                }
                return stateMap;

            }

            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf byteBuf, @NotNull HashMap<BlockPos, SpecificRealGasState> stateMap) {
                byteBuf.writeInt(stateMap.size());
                stateMap.forEach((key, value) -> {
                    byteBuf.writeBlockPos(key);
                    byteBuf.writeFloat(value.pressure());
                    byteBuf.writeFloat(value.specificEnthalpy());
                    byteBuf.writeFloat(value.temperature());
                    byteBuf.writeFloat(value.specificEntropy());
                    byteBuf.writeFloat(value.vaporQuality());
                });
            }
        };
    }

    @Override
    public @NotNull HashMap<BlockPos, SpecificRealGasState> copy(@NotNull HashMap<BlockPos, SpecificRealGasState> stateMap) {
        return stateMap;
    }
}
