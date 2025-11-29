package com.rae.crowns.content.event;

import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.rae.crowns.init.misc.CommandsInit;
import com.rae.crowns.init.misc.DamageSourceInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber()
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
    public static void onEntityTick(LivingEvent.@NotNull LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level() instanceof  ServerLevel level) {
            PhysicsWorldData data = PhysicsSaveManager.get((ServerLevel) entity.level());
            AtomicReference<Float> cumlTemp = new AtomicReference<>(0f);
            AtomicReference<Integer> numberOfTemps = new AtomicReference<>(0);
            BlockPos.betweenClosedStream(entity.getBoundingBox()).forEach(blockPos -> {
                SectionPos sectionPos = SectionPos.of(blockPos);
                cumlTemp.set(cumlTemp.get() + getTemperature(data.getLayer(DataLayerType.TEMPERATURE,sectionPos.asLong()),blockPos));
                numberOfTemps.set(numberOfTemps.get() + 1);
            });

            if (numberOfTemps.get() > 0) {
                float temp = cumlTemp.get() / numberOfTemps.get();
                if (temp > 450){
                    entity.hurt(DamageSourceInit.over_heat(level), 1.0f);
                    entity.setRemainingFireTicks(20);
                    //entity.lavaHurt();
                }
                if (temp < 200){
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
}
