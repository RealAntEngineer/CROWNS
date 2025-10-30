package com.rae.crowns.mixin;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.*;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @Inject(method = "write", at = @At("RETURN"), cancellable = true)
    private static void onWriteInject(
            ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {

        CompoundTag root = cir.getReturnValue();
        ListTag sections = root.getList("sections", Tag.TAG_COMPOUND);

        TemperatureWorldData worldData = TemperatureManager.get(level);

        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sectionTag = sections.getCompound(i);

            if (!sectionTag.contains("Y")) continue;
            int y = sectionTag.getByte("Y");
            long sectionPos = SectionPos.of(chunk.getPos(), y).asLong();
            TemperatureDataLayer temp = worldData.getTemperature(sectionPos);
            ConductionDataLayer cond = worldData.getConduction(sectionPos);
            ResilienceDataLayer resilience = worldData.getResilience(sectionPos);


            if (temp != null && cond != null && resilience != null) {
                sectionTag.putByteArray("Temperature", temp.toBytes());
                sectionTag.putByteArray("Conduction", cond.toBytes());
                sectionTag.putByteArray("Resilience", resilience.toBytes());
                sectionTag.putBoolean("TemperatureDirty", worldData.isDirty(sectionPos));
                sectionTag.putInt("ThermalDataVersion", TemperatureWorldData.DATA_VERSION);

                //we can save and not unload the chunk. so we need to check if it was unloaded or not.
                if (!worldData.isLoaded(sectionPos)){
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
            ServerLevel level, PoiManager p_188232_, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        ListTag sections = tag.getList("sections", Tag.TAG_COMPOUND);

        TemperatureWorldData worldData = TemperatureManager.get(level);

        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sectionTag = sections.getCompound(i);

            if (!sectionTag.contains("Y")) continue;
            int y = sectionTag.getByte("Y");
            long sectionPos = SectionPos.of(pos, y).asLong();
            if (sectionTag.contains("ThermalDataVersion") && sectionTag.getInt("ThermalDataVersion") == TemperatureWorldData.DATA_VERSION &&
                    sectionTag.contains("Temperature") && sectionTag.contains("Resilience") && sectionTag.contains("Conduction")) {

                byte[] tempBytes = sectionTag.getByteArray("Temperature");
                byte[] condBytes = sectionTag.getByteArray("Conduction");
                byte[] resBytes = sectionTag.getByteArray("Resilience");

                worldData.put(sectionPos, TemperatureDataLayer.fromBytes(tempBytes), ConductionDataLayer.fromBytes(condBytes),
                        ResilienceDataLayer.fromBytes(resBytes));
                CROWNS.LOGGER.info("loading section : {}", SectionPos.of(pos, y));

                if (sectionTag.contains("TemperatureDirty") && sectionTag.getBoolean("TemperatureDirty")) {
                    worldData.setDirty(sectionPos);
                } else {
                    worldData.setClean(sectionPos);
                }
            } else {
                //worldData.putForInitialisation(sectionPos);
            }

        }
    }
}
