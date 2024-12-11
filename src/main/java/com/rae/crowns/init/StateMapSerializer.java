package com.rae.crowns.init;

import com.rae.crowns.api.thermal_utilities.SpecificRealGazState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class StateMapSerializer implements EntityDataSerializer<HashMap<BlockPos, SpecificRealGazState>> {
    public StateMapSerializer() {
    }

    @Override
    public void write(FriendlyByteBuf byteBuf, HashMap<BlockPos, SpecificRealGazState> stateMap) {
        byteBuf.writeInt(stateMap.size());
        stateMap.forEach((key, value)->{
            byteBuf.writeBlockPos(key);
            byteBuf.writeFloat(value.temperature());
            byteBuf.writeFloat(value.pressure());
            byteBuf.writeFloat(value.specificEnthalpy());
            byteBuf.writeFloat(value.vaporQuality());
        });

    }

    @Override
    public @NotNull HashMap<BlockPos, SpecificRealGazState> read(FriendlyByteBuf byteBuf) {
        HashMap<BlockPos, SpecificRealGazState> stateMap = new HashMap<>();
        int size = byteBuf.readInt();
        for (int i = 0; i < size; i++) {
            stateMap.put(byteBuf.readBlockPos(), new SpecificRealGazState(byteBuf.readFloat(),byteBuf.readFloat(),byteBuf.readFloat(),byteBuf.readFloat()));

        }
        return stateMap;
    }

    @Override
    public @NotNull HashMap<BlockPos, SpecificRealGazState> copy(@NotNull HashMap<BlockPos, SpecificRealGazState> stateMap) {
        return stateMap;
    }
}
