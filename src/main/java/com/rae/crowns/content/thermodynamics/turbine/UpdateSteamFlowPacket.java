package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.data.PacketInit;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;


public class UpdateSteamFlowPacket implements ClientboundPacketPayload {


    public static StreamCodec<FriendlyByteBuf, UpdateSteamFlowPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> packet.write(buf),
            UpdateSteamFlowPacket::new
    );

    private final CompoundTag tag;

    // Construct from server data
    public UpdateSteamFlowPacket(SteamFlowData savedData) {
        this.tag = savedData.save(new CompoundTag());
    }

    // Construct from network buffer
    public UpdateSteamFlowPacket(FriendlyByteBuf buffer) {
        this.tag = buffer.readNbt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeNbt(tag);
    }

    @Override
    public void handle(LocalPlayer player) {
        SteamFlowData clientData = SteamFlowData.load(tag);
        SteamFlowManager.setSavedData(clientData);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return PacketInit.UPDATE_STEAM_FLOW;
    }
}

