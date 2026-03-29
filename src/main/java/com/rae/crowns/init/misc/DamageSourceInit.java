package com.rae.crowns.init.misc;

import com.rae.crowns.CROWNS;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

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

}
