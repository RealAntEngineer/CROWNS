package com.rae.crowns.content.fields.temperature;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;

public class UpdateSectionsPacket extends SimplePacketBase {
    private final Map<SectionPos, TemperatureDataLayer> temperatureMap;


    public UpdateSectionsPacket(Map<SectionPos, TemperatureDataLayer> temperatureMap) {
        this.temperatureMap = temperatureMap;
    }
    public UpdateSectionsPacket(FriendlyByteBuf buffer) {
        // decode
        this.temperatureMap = buffer.readMap(
                buf -> SectionPos.of(buf.readLong()),                // key reader
                buf -> TemperatureDataLayer.fromBytes(buf.readByteArray()) // value reader
        );
    }
    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeMap(
                temperatureMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );    }

    @Override
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                LocalTemperatureData.receiveUpdate(temperatureMap);
            }
        });
        return true;
    }
}
