package com.rae.crowns.init.data;


import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.util.UpdateSectionsPacket;
import com.rae.crowns.content.thermodynamics.turbine.UpdateSteamFlowPacket;
import com.simibubi.create.Create;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Locale;

public enum PacketInit implements BasePacketPayload.PacketTypeProvider {
    UPDATE_STEAM_FLOW(UpdateSteamFlowPacket.class, UpdateSteamFlowPacket.STREAM_CODEC),
    UPDATE_SECTIONS(UpdateSectionsPacket.class, UpdateSectionsPacket.STREAM_CODEC);

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> PacketInit(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = this.name().toLowerCase(Locale.ROOT);
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(Create.asResource(name)),
                clazz, codec
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(CROWNS.MODID, 1);
        for (PacketInit packet : PacketInit.values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }
}
