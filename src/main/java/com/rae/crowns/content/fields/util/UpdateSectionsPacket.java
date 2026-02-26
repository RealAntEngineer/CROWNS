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
    //private final Map<SectionPos, VelocityDataLayer> vxMap;
    //private final Map<SectionPos, VelocityDataLayer> vyMap;
    //private final Map<SectionPos, VelocityDataLayer> vzMap;


    public UpdateSectionsPacket(Map<SectionPos, TemperatureDataLayer> temperatureMap
                                /*Map<SectionPos, VelocityDataLayer> vxMap,
                                Map<SectionPos, VelocityDataLayer> vyMap,
                                Map<SectionPos, VelocityDataLayer> vzMap*/) {
        this.temperatureMap = temperatureMap;
        //this.vxMap = vxMap;
        //this.vyMap = vyMap;
        //this.vzMap = vzMap;

    }

    public UpdateSectionsPacket(@NotNull FriendlyByteBuf buffer) {
        // decode
        this.temperatureMap = buffer.readMap(
                buf -> SectionPos.of(buf.readLong()),                // key reader
                buf -> new TemperatureDataLayer().fromBytes(buf.readByteArray()) // value reader
        );
        /*
        this.vxMap = buffer.readMap(
                buf -> SectionPos.of(buf.readLong()),                // key reader
                buf -> new VelocityDataLayer().fromBytes(buf.readByteArray()) // value reader
        );

        this.vyMap = buffer.readMap(
                buf -> SectionPos.of(buf.readLong()),                // key reader
                buf -> new VelocityDataLayer().fromBytes(buf.readByteArray()) // value reader
        );

        this.vzMap = buffer.readMap(
                buf -> SectionPos.of(buf.readLong()),                // key reader
                buf -> new VelocityDataLayer().fromBytes(buf.readByteArray()) // value reader
        );*/
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeMap(
                temperatureMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );
        /*
        buffer.writeMap(
                vxMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );
        buffer.writeMap(
                vyMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );
        buffer.writeMap(
                vzMap,
                (buf, pos) -> buf.writeLong(pos.asLong()),           // key writer
                (buf, layer) -> buf.writeByteArray(layer.toBytes())  // value writer
        );*/
    }

    @Override
    public boolean handle(NetworkEvent.@NotNull Context context) {
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return;

                long time = mc.level.getGameTime();
                LocalPhysicData.receiveUpdate(temperatureMap, /*vxMap, vyMap, vzMap,*/ time);
            }
        });
        return true;
    }
}
