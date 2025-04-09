package com.rae.crowns.content.fields.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;

public class RadiationManager {
    private static final Queue<BlockPos> updateQueue = new ArrayDeque<>();
    private static final int MAX_UPDATES_PER_TICK = 100;
    private static final Map<ServerLevel, RadiationWorldData> worldDataMap = new WeakHashMap<>();

    public static void enqueue(ServerLevel level, BlockPos pos) {
        updateQueue.add(pos.immutable());
    }

    public static void tick(ServerLevel level) {
        RadiationWorldData data = worldDataMap.computeIfAbsent(level, k -> new RadiationWorldData());

        for (int i = 0; i < MAX_UPDATES_PER_TICK && !updateQueue.isEmpty(); i++) {
            BlockPos pos = updateQueue.poll();
            if (!level.isLoaded(pos)) continue;

            SectionPos sectionPos = SectionPos.of(pos);
            RadiationData rad = data.getOrCreate(sectionPos);

            int cx = pos.getX() & 15;
            int cy = pos.getY() & 15;
            int cz = pos.getZ() & 15;

            short current = rad.getRadiation(cx, cy, cz);
            short decayed = (short)(current * 0.99f);
            rad.setRadiation(cx, cy, cz, decayed);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!level.isLoaded(neighbor)) continue;

                SectionPos neighborSection = SectionPos.of(neighbor);
                RadiationData neighborRad = data.getOrCreate(neighborSection);

                int nx = neighbor.getX() & 15;
                int ny = neighbor.getY() & 15;
                int nz = neighbor.getZ() & 15;

                short neighborValue = neighborRad.getRadiation(nx, ny, nz);
                int diff = current - neighborValue;
                if (Math.abs(diff) > 1) {
                    short transfer = (short)(diff * 0.05f);
                    rad.setRadiation(cx, cy, cz, (short)(current - transfer));
                    neighborRad.setRadiation(nx, ny, nz, (short)(neighborValue + transfer));

                    enqueue(level, neighbor);
                }
            }
        }
    }
}
