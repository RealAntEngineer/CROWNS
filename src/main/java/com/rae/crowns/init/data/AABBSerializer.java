package com.rae.crowns.init.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
// TODO put this in the formic API
public class AABBSerializer implements EntityDataSerializer<AABB> {

    StreamCodec<RegistryFriendlyByteBuf, AABB> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, aabb -> aabb.minX,
            ByteBufCodecs.DOUBLE, aabb -> aabb.minY,
            ByteBufCodecs.DOUBLE, aabb -> aabb.minZ,
            ByteBufCodecs.DOUBLE, aabb -> aabb.maxX,
            ByteBufCodecs.DOUBLE, aabb -> aabb.maxY,
            ByteBufCodecs.DOUBLE, aabb -> aabb.maxZ,
            AABB::new
            ) ;
    public AABBSerializer() {
    }

    @Override
    public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, AABB> codec() {
        return CODEC;
    }

    @Override
    public @NotNull AABB copy(@NotNull AABB aabb) {
        return aabb;
    }
}
