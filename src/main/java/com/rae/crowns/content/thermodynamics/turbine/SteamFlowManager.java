package com.rae.crowns.content.thermodynamics.turbine;

import com.rae.crowns.init.data.PacketInit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SteamFlowManager {

    static @Nullable SteamFlowData storage = null;

    public static void addSteamCurrent(ServerLevel level, SteamCurrent steamCurrent) {
        if (storage == null) return;
        storage.steamCurrents.computeIfAbsent(level.dimension().location(), d -> new ArrayList<>())
                .add(steamCurrent);
        storage.setDirty(); // replace with markDirty() if your class uses that name

    }

    public static void tick(@NotNull Level world) {
        if (storage == null) {
            return;
        }
        if (!storage.steamCurrents.containsKey(world.dimension().location())) {
            storage.steamCurrents.put(world.dimension().location(), new ArrayList<>());
        }
        storage.steamCurrents.get(world.dimension().location())
                .removeIf(steamCurrent -> {
                    if (steamCurrent == null) {
                        return true;
                    }
                    return false;
                });
        //there shouldn't be null values here.
        storage.steamCurrents.get(world.dimension().location())
                .forEach(steamCurrent -> steamCurrent.tick(world));

        if (world instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                PacketInit.getChannel()
                        .send(PacketDistributor.PLAYER.with(() -> player),
                                new UpdateSteamFlowPacket(storage));
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

    /*@OnlyIn(Dist.CLIENT)
    public static void render(ClientLevel level) {

    }*/

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
            return;
        }
        storage.steamCurrents.clear();
    }
}
