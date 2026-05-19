package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.fields.util.client.LocalPhysicData;
import com.rae.crowns.init.data.PacketInit;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class UpdateSectionsPacket implements ClientboundPacketPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, UpdateSectionsPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> packet.write(buf),
            UpdateSectionsPacket::new
    );
    private final Map<SectionPos, TemperatureDataLayer>                      temperatureMap;

    public UpdateSectionsPacket(Map<SectionPos, TemperatureDataLayer> temperatureMap) {
        this.temperatureMap = temperatureMap;

    }

    public UpdateSectionsPacket(@NotNull FriendlyByteBuf buffer) {
        // decode
        this.temperatureMap = buffer.readMap(
                buf -> SectionPos.of(buf.readLong()),                // key reader
                buf -> new TemperatureDataLayer().fromBytes(buf.readByteArray()) // value reader
        );
    }

    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeMap(
                temperatureMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );
    }

    @Override
    public void handle(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long time = mc.level.getGameTime();
        LocalPhysicData.receiveUpdate(temperatureMap, time);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return PacketInit.UPDATE_SECTIONS;
    }
}
