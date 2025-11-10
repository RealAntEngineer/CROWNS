package com.rae.crowns.mixin;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.*;
import com.rae.crowns.content.fields.util.AbstractDataLayer;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @Inject(method = "write", at = @At("RETURN"), cancellable = true)
    private static void onWriteInject(
            @NotNull ServerLevel level, @NotNull ChunkAccess chunk, @NotNull CallbackInfoReturnable<CompoundTag> cir) {

        CompoundTag root = cir.getReturnValue();
        ListTag sections = root.getList("sections", Tag.TAG_COMPOUND);

        PhysicsWorldData worldData = PhysicsSaveManager.get(level);

        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sectionTag = sections.getCompound(i);

            if (!sectionTag.contains("Y")) continue;
            int y = sectionTag.getByte("Y");
            long sectionPos = SectionPos.of(chunk.getPos(), y).asLong();

            // --- Iterate over all registered DataLayerTypes ---

            boolean exist = false;
            for (DataLayerType<?> type : DataLayerType.REGISTRY.values()) {
                AbstractDataLayer layer = worldData.getLayer(type, sectionPos);
                if (layer != null) {
                    sectionTag.putByteArray(type.id, layer.toBytes());
                    exist = true;
                }
            }
            if (exist) {
                sectionTag.putBoolean("TemperatureDirty", worldData.isDirty(sectionPos));
                sectionTag.putInt("ThermalDataVersion", PhysicsWorldData.DATA_VERSION);


                if (!worldData.isLoaded(sectionPos)) {
                    worldData.dumpSection(sectionPos);
                    CROWNS.LOGGER.info("unloading section : {}", SectionPos.of(chunk.getPos(), y));
                }
            }
            sections.set(i, sectionTag);
        }

        root.put("sections", sections);
        cir.setReturnValue(root);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void onReadInject(
            @NotNull ServerLevel level, PoiManager poiManager, @NotNull ChunkPos pos, @NotNull CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {

        ListTag sections = tag.getList("sections", Tag.TAG_COMPOUND);
        PhysicsWorldData worldData = PhysicsSaveManager.get(level);

        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sectionTag = sections.getCompound(i);

            if (!sectionTag.contains("Y")) continue;
            int y = sectionTag.getByte("Y");
            long sectionPos = SectionPos.of(pos, y).asLong();

            if (!sectionTag.contains("ThermalDataVersion") ||
                    sectionTag.getInt("ThermalDataVersion") != PhysicsWorldData.DATA_VERSION) continue;

            // --- Iterate over all registered DataLayerTypes ---
            Map<DataLayerType<?>, AbstractDataLayer> layers = new HashMap<>();
            for (DataLayerType<?> type : DataLayerType.REGISTRY.values()) {
                if (sectionTag.contains(type.id)) {
                    byte[] bytes = sectionTag.getByteArray(type.id);
                    AbstractDataLayer layer = type.createLayer().fromBytes(bytes);
                    layers.put(type, layer);
                }
            }

            // Put loaded layers into world data
            for (Map.Entry<DataLayerType<?>, AbstractDataLayer> entry : layers.entrySet()) {
                worldData.putLayer(entry.getKey(), sectionPos, entry.getValue());
            }

            if (sectionTag.contains("TemperatureDirty") && sectionTag.getBoolean("TemperatureDirty")) {
                worldData.setDirty(sectionPos);
            } else {
                worldData.setClean(sectionPos);
            }

            CROWNS.LOGGER.info("loading section : {}", SectionPos.of(pos, y));
        }
    }
}

