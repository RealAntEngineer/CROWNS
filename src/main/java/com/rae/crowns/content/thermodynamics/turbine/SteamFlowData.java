package com.rae.crowns.content.thermodynamics.turbine;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SteamFlowData extends SavedData {
    static final Codec<List<ResourceLocation>> KEYS_CODEC = Codec.list(ResourceLocation.CODEC);
    Map<ResourceLocation, List<SteamCurrent>> steamCurrents = new HashMap<>();

    public static @NotNull SteamFlowData load(@NotNull CompoundTag nbt) {
        SteamFlowData savedData = new SteamFlowData();
        List<ResourceLocation> dimensionKeys = KEYS_CODEC.parse(NbtOps.INSTANCE, nbt.get("dimensions")).result().orElse(List.of());

        for (ResourceLocation key : dimensionKeys) {

            savedData.steamCurrents.put(key, new ArrayList<>());
            ListTag currentsNBT = nbt.getList(key.toString(), 10);
            currentsNBT.forEach(dimensionTag -> {
                savedData.steamCurrents.get(key).add(SteamCurrent.fromNBT((CompoundTag) dimensionTag));
            });
        }

        return savedData;
    }

    public static @NotNull SteamFlowData loadData(@NotNull MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(SteamFlowData::load, SteamFlowData::new, "steam_currents");
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        nbt.put("dimensions", KEYS_CODEC.encodeStart(NbtOps.INSTANCE, steamCurrents.keySet().stream().toList()).result().orElse(new CompoundTag()));
        for (Map.Entry<ResourceLocation, List<SteamCurrent>> entry : steamCurrents.entrySet()) {

            ListTag tag = new ListTag();

            for (SteamCurrent current : entry.getValue()) {
                tag.add(current.toNBT()); // assuming you have a toNBT() method in SteamCurrent
            }

            nbt.put(entry.getKey().toString(), tag);
        }

        return nbt;
    }
}
