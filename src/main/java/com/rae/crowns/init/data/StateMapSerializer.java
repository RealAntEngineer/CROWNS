package com.rae.crowns.init.data;

import com.rae.formicapi.thermal_utilities.SpecificRealGazState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

// TODO put this in the formic API
public class StateMapSerializer implements EntityDataSerializer<HashMap<BlockPos, SpecificRealGazState>> {
    public StateMapSerializer() {
    }

    @Override
    public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, HashMap<BlockPos, SpecificRealGazState>> codec() {
        return new StreamCodec<>() {
            @Override
            public @NotNull HashMap<BlockPos, SpecificRealGazState> decode(@NotNull RegistryFriendlyByteBuf buffer) {
                HashMap<BlockPos, SpecificRealGazState> stateMap = new HashMap<>();
                int                                     size     = buffer.readInt();
                for (int i = 0; i < size; i++) {
                    stateMap.put(buffer.readBlockPos(), new SpecificRealGazState(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

                }
                return stateMap;

            }

            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf byteBuf, @NotNull HashMap<BlockPos, SpecificRealGazState> stateMap) {
                byteBuf.writeInt(stateMap.size());
                stateMap.forEach((key, value) -> {
                    byteBuf.writeBlockPos(key);
                    byteBuf.writeFloat(value.temperature());
                    byteBuf.writeFloat(value.pressure());
                    byteBuf.writeFloat(value.specificEnthalpy());
                    byteBuf.writeFloat(value.vaporQuality());
                });
            }
        };
    }

    @Override
    public @NotNull HashMap<BlockPos, SpecificRealGazState> copy(@NotNull HashMap<BlockPos, SpecificRealGazState> stateMap) {
        return stateMap;
    }
}
