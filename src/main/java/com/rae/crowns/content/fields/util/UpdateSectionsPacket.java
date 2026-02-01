package com.rae.crowns.content.fields.util;

import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class UpdateSectionsPacket extends SimplePacketBase {
    private final Map<SectionPos, TemperatureDataLayer> temperatureMap;


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

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeMap(
                temperatureMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );
    }

    @Override
    public boolean handle(NetworkEvent.@NotNull Context context) {
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return;

                long time = mc.level.getGameTime();
                LocalPhysicData.receiveUpdate(temperatureMap, time);
            }
        });
        return true;
    }
}
