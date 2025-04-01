package com.rae.colony_api.data;

import com.rae.colony_api.data.managers.FloatMapDataLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
@Mod.EventBusSubscriber()
public class Event {
    private static final Logger LOGGER = LogManager.getLogger();
    private static RegistryAccess.Frozen registryAccess = null;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("getting the server registry access");
        registryAccess = event.getServer().registryAccess();
    }

    /**
     *
     * @param registryKey the registry you want to access
     * @return the synced registry
     * @param <T> the type of object the registry is for
     */
    public static <T> Registry<T> getSideAwareRegistry(ResourceKey<Registry<T>> registryKey) {
        if (registryAccess != null) {
            return registryAccess.registryOrThrow(registryKey);
        } else {
            LOGGER.debug("getting the registry access from the client");
            return Objects.requireNonNull(Minecraft.getInstance().getConnection())
                    .registryAccess().registry(registryKey)
                    .orElseThrow();
        }
    }
}
