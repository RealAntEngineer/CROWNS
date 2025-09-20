package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.data.PacketInit;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;


public class UpdateSteamFlowPacket implements ClientboundPacketPayload {


    public static StreamCodec<RegistryFriendlyByteBuf, UpdateSteamFlowPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> packet.write(buf),
            UpdateSteamFlowPacket::new
    );

    private final CompoundTag tag;

    // Construct from server data
    public UpdateSteamFlowPacket(SteamFlowData savedData, HolderLookup.Provider registries) {
        this.tag = savedData.save(new CompoundTag(), registries);
    }

    // Construct from network buffer
    public UpdateSteamFlowPacket(RegistryFriendlyByteBuf buffer) {
        this.tag = buffer.readNbt();
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeNbt(tag);
    }

    @Override
    public void handle(LocalPlayer player) {
        SteamFlowData clientData = SteamFlowData.load(tag, player.registryAccess());
        SteamFlowManager.setSavedData(clientData);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return PacketInit.UPDATE_STEAM_FLOW;
    }
}

