package com.rae.crowns.content.fields.temperature;

import com.rae.crowns.init.misc.PacketInit;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;

public class UpdateSectionsPacket implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSectionsPacket> STREAM_CODEC = StreamCodec.of(
            (byteBuf, packet) -> packet.write(byteBuf),
            UpdateSectionsPacket::new);

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

    public void write(FriendlyByteBuf buffer) {
        buffer.writeMap(
                temperatureMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );    }


    @Override
    public void handle(LocalPlayer player) {
        LocalTemperatureData.receiveUpdate(temperatureMap);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return PacketInit.UPDATE_SAVED_DATA;
    }
}
