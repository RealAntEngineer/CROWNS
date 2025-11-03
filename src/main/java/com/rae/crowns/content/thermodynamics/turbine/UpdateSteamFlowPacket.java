package com.rae.crowns.content.thermodynamics.turbine;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UpdateSteamFlowPacket extends SimplePacketBase {

    private final @Nullable CompoundTag tag;

    // Construct from server data
    public UpdateSteamFlowPacket(@NotNull SteamFlowData savedData) {
        this.tag = savedData.save(new CompoundTag());
    }

    // Construct from network buffer
    public UpdateSteamFlowPacket(@NotNull FriendlyByteBuf buffer) {
        this.tag = buffer.readNbt();
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeNbt(tag);
    }

    @Override
    public boolean handle(NetworkEvent.@NotNull Context context) {
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                SteamFlowData clientData = SteamFlowData.load(tag);
                SteamFlowManager.setSavedData(clientData);
            }
        });
        return true;
    }
}

