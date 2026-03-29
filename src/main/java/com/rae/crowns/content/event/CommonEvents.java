package com.rae.crowns.content.event;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.content.thermodynamics.compressor.CompressorBlockEntity;
import com.rae.crowns.content.thermodynamics.conduction.HeatExchangerBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamCollectorBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.SteamInputBlockEntity;
import com.rae.crowns.init.misc.CommandsInit;
import com.rae.crowns.init.misc.DamageSourceInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

@EventBusSubscriber(modid = CROWNS.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedOutEvent event) {
        //System.out.println(event.getEntity().getServer());
    }

    @SubscribeEvent
    public static void registerCommands(@NotNull RegisterCommandsEvent event) {
        CommandsInit.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEntityTick(@NotNull EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level && entity instanceof LivingEntity) {
            PhysicsWorldData data = PhysicsSaveManager.get((ServerLevel) entity.level());
            if (data == null) return;
            AtomicReference<Float> cumlTemp = new AtomicReference<>(0f);
            AtomicReference<Integer> numberOfTemps = new AtomicReference<>(0);
            BlockPos.betweenClosedStream(entity.getBoundingBox()).forEach(blockPos -> {
                SectionPos sectionPos = SectionPos.of(blockPos);
                cumlTemp.set(cumlTemp.get() + getTemperature(data.getLayer(DataLayerType.TEMPERATURE, sectionPos.asLong()), blockPos));
                numberOfTemps.set(numberOfTemps.get() + 1);
            });

            if (numberOfTemps.get() > 0) {
                float temp = cumlTemp.get() / numberOfTemps.get();
                if (temp > 450) {
                    entity.hurt(DamageSourceInit.over_heat(level), 1.0f);
                    entity.setRemainingFireTicks(20);
                    //entity.lavaHurt();
                }
                if (temp < 200) {
                    entity.hurt(DamageSourceInit.freezing(level), 1.0f);
                    entity.setTicksFrozen(20);
                }
            }

        }
    }

    private static float getTemperature(@Nullable TemperatureDataLayer layer, @NotNull Vec3i pos) {

        if (layer == null) return 300;

        // Convert world coordinates to local (0–15) section coordinates
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        return layer.get(localX, localY, localZ);
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            SteamCollectorBlockEntity.registerCapabilities(event);
            SteamInputBlockEntity.registerCapabilities(event);
            CompressorBlockEntity.registerCapabilities(event);
            HeatExchangerBlockEntity.registerCapabilities(event);
        }

    }
}
