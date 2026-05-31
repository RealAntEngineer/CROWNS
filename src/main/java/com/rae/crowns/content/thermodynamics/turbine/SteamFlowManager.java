package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SteamFlowManager {

    static @Nullable SteamFlowData storage = null;

    public static void addSteamCurrent(ServerLevel level, SteamCurrent steamCurrent) {
        if (storage == null) return;
        //we need to verify that there is no steam current already present where we create ours.
        storage.steamCurrents.computeIfAbsent(level.dimension().location(), d -> new ArrayList<>())
                .add(steamCurrent);
        storage.setDirty();

    }

    public static void tick(@NotNull Level world) {
        if (storage == null) {
            return;
        }
        if (!storage.steamCurrents.containsKey(world.dimension().location())) {
            storage.steamCurrents.put(world.dimension().location(), new ArrayList<>());
        }
        int before = getSFAmount();
        storage.steamCurrents.getOrDefault(world.dimension().location(), List.of())
                .removeIf(Objects::isNull);//ensure no Null Values
        int after = getSFAmount();

        if (before > after) {
            CROWNS.LOGGER.warn("Detected and cleared {} null value in the steam currents storage", before - after);
        }

        if (world.getGameTime() % 40 == 0) {
            before = getSFAmount();
            storage.steamCurrents.get(world.dimension().location())
                    .removeIf(steamCurrent -> !steamCurrent.isValid(world));
            after = getSFAmount();
            if (before > after) {
                CROWNS.LOGGER.warn("Detected and cleared {} invalid steam currents", before - after);
            }

        }
        //there shouldn't be null values here.
        storage.steamCurrents.get(world.dimension().location())
                .forEach(steamCurrent -> steamCurrent.tick(world));

        if (after > 500){
            CROWNS.LOGGER.warn("WARNING there is too much steam current on the server side to safely sync to client :  {}", after);
        } else {
            if (world instanceof ServerLevel serverLevel) {
                for (ServerPlayer player : serverLevel.players()) {
                    CatnipServices.NETWORK.sendToClientsTrackingAndSelf(player,
                            new UpdateSteamFlowPacket(storage, world.registryAccess()));
                }
            }
        }


    }

    public static @NotNull List<SteamCurrent> getCurrentsInBounds(ServerLevel level, @NotNull AABB bound) {
        List<SteamCurrent> collector = new ArrayList<>();
        if (storage == null) return collector;
        storage.steamCurrents.getOrDefault(level.dimension().location(), List.of()).forEach((steamCurrent) ->
        {
            if (steamCurrent.intersects(bound)) {
                collector.add(steamCurrent);
                steamCurrent.rebuild(level);
            }
        });
        return collector;
    }


    public static void serverStarted(@Nullable MinecraftServer server) {
        if (server == null)
            return;
        storage = SteamFlowData.loadData(server);

    }

    public static void playerLoaded(ServerPlayer player) {

    }

    public static void setSavedData(@NotNull SteamFlowData savedData) {
        if (storage == null) {
            storage = savedData;
        } else {
            //attention : if the server is local, server only data will get overwritten.
            storage.steamCurrents = savedData.steamCurrents;
        }
    }

    public static void clear() {
        if (storage == null) {
            CROWNS.LOGGER.warn("Trying to clear steam currents with null storage");
            return;
        }
        int total = getSFAmount();

        CROWNS.LOGGER.info("Cleared {} steam currents across {} all dimensions",
                total, storage.steamCurrents.size());
        storage.steamCurrents.clear();
    }

    public static int getSFAmount() {
        if (storage == null) {
            return 0;
        }
        return storage.steamCurrents.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }
}
