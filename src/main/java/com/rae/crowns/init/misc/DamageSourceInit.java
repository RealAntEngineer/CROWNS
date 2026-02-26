package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import com.simibubi.create.foundation.damageTypes.DamageTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;

public class DamageSourceInit {
    public static final ResourceKey<DamageType>
            HIGH_TEMPERATURE = key("high_temperature");

    public static final ResourceKey<DamageType>
            LOW_TEMPERATURE = key("low_temperature");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, CROWNS.resource(name));
    }

    public static DamageSource over_heat(Level level) {
        return source(DamageSourceInit.HIGH_TEMPERATURE, level);
    }

    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(key));
    }

    public static DamageSource freezing(Level level) {
        return source(DamageSourceInit.LOW_TEMPERATURE, level);
    }

    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level, @Nullable Entity entity) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(key), entity);
    }

    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level, @Nullable Entity causingEntity, @Nullable Entity directEntity) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(key), causingEntity, directEntity);
    }


    //todo if we use that we can avoid the experimental warning
    public static void bootstrap(BootstapContext<DamageType> ctx) {
        new DamageTypeBuilder(HIGH_TEMPERATURE)
                .scaling(DamageScaling.NEVER)
                .effects(DamageEffects.DROWNING)
                .exhaustion(0)
                .deathMessageType(DeathMessageType.DEFAULT)
                .msgId("creatingspace.no_oxygen")
                .register(ctx);
    }

}
