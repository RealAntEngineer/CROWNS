package com.rae.crowns.init;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AABBSerializer implements EntityDataSerializer<AABB> {
    public AABBSerializer() {
    }

    @Override
    public void write(FriendlyByteBuf byteBuf, AABB aabb) {
        byteBuf.writeDouble(aabb.minX);
        byteBuf.writeDouble(aabb.minY);
        byteBuf.writeDouble(aabb.minZ);
        byteBuf.writeDouble(aabb.maxX);
        byteBuf.writeDouble(aabb.maxY);
        byteBuf.writeDouble(aabb.maxZ);
    }

    @Override
    public @NotNull AABB read(FriendlyByteBuf byteBuf) {
        return new AABB(byteBuf.readDouble(), byteBuf.readDouble(), byteBuf.readDouble(),byteBuf.readDouble(),byteBuf.readDouble(),byteBuf.readDouble());
    }

    @Override
    public @NotNull AABB copy(@NotNull AABB aabb) {
        return aabb;
    }
}
