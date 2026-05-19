package com.rae.crowns.content.thermodynamics.turbine;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
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
    //static Codec<Map<ResourceLocation,List<SteamCurrent>>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC,Codec.list(SteamCurrent.CODEC));
    static final Codec<List<ResourceLocation>> KEYS_CODEC = Codec.list(ResourceLocation.CODEC);
    Map<ResourceLocation, List<SteamCurrent>> steamCurrents = new HashMap<>();

    public static SteamFlowData loadData(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), "steam_currents");
    }

    public static SavedData.Factory<SteamFlowData> factory() {
        return new SavedData.Factory<>(SteamFlowData::new, SteamFlowData::load);
    }

    public static SteamFlowData load(CompoundTag nbt, HolderLookup.Provider provider) {
        SteamFlowData          savedData     = new SteamFlowData();
        List<ResourceLocation> dimensionKeys = KEYS_CODEC.parse(NbtOps.INSTANCE, nbt.get("dimensions")).result().orElse(List.of());

        for (ResourceLocation key : dimensionKeys) {

            savedData.steamCurrents.put(key, new ArrayList<>());
            ListTag currentsNBT = nbt.getList(key.toString(), 10);
            currentsNBT.forEach(dimensionTag -> {
                savedData.steamCurrents.get(key).add(SteamCurrent.fromNBT((CompoundTag) dimensionTag));
            });
        }
        /*savedData.steamCurrents = new HashMap<>(CODEC.parse(NbtOps.INSTANCE, nbt.get("currents"))
                .result().orElse(new HashMap<>()));*/
        return savedData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
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
