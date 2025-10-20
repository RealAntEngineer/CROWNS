package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureManager;
import com.rae.crowns.content.fields.temperature.TemperatureWorldData;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = CROWNS.MODID)
public class DataEvents {

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ChunkAccess chunk = event.getChunk();
            TemperatureWorldData worldData = TemperatureManager.get(serverLevel);

            // Dump all sections for this chunk
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;

            for (int sectionY = 0; sectionY < chunk.getSectionsCount(); sectionY++) {
                SectionPos sectionPos = SectionPos.of(chunkX, sectionY, chunkZ);
                worldData.dumpSection(sectionPos);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TemperatureWorldData worldData = TemperatureManager.get(serverLevel);
            if (event.isNewChunk()) {//warning if it's an old world for
                ChunkAccess chunk = event.getChunk();
                ChunkPos chunkPos = chunk.getPos();
                for (int i = chunk.getMinSection(); i < chunk.getMaxSection(); i++) {
                    worldData.putForInitialisation(SectionPos.of(chunkPos, i).asLong());
                }
            }
        }

    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer)
            SteamFlowManager.playerLoaded(serverPlayer);

    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SteamFlowManager.serverStarted(event.getServer());
    }
}
