package com.rae.crowns.init.data;

import com.rae.formicapi.content.thermal_utilities.SpecificRealGasState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import org.lwjgl.system.NonnullDefault;

import java.util.HashMap;

@NonnullDefault
public class StateMapSerializer implements EntityDataSerializer<HashMap<BlockPos, SpecificRealGasState>> {
    public StateMapSerializer() {
    }

    @Override
    public void write(FriendlyByteBuf byteBuf, HashMap<BlockPos, SpecificRealGasState> stateMap) {
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

    @Override
    public HashMap<BlockPos, SpecificRealGasState> read(FriendlyByteBuf byteBuf) {
        HashMap<BlockPos, SpecificRealGasState> stateMap = new HashMap<>();
        int size = byteBuf.readInt();
        for (int i = 0; i < size; i++) {
            stateMap.put(byteBuf.readBlockPos(), new SpecificRealGasState(byteBuf.readFloat(), byteBuf.readFloat(),byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat()));

        }
        return stateMap;
    }

    @Override
    public HashMap<BlockPos, SpecificRealGasState> copy(HashMap<BlockPos, SpecificRealGasState> stateMap) {
        return stateMap;
    }
}
