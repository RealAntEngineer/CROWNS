package com.rae.crowns.content.hazards.radiation.pointsource;

import com.rae.crowns.content.hazards.HazardEntry;
import com.rae.crowns.content.hazards.HazardSystem;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import com.rae.crowns.init.misc.TagsInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class PointSourceUtil {
    /**
     * Finds radioactive blocks within a certain radius of the player
     *
     * @param player Target player
     * @param radius Radius of the sphere in which blocks are checked
     * @return HashMap<BlockPos, HazardEntry> of all radioactive blocks
     **/
    public static HashMap<BlockPos, HazardEntry> findRadBlocks(Player player, int radius) {
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        HashMap<BlockPos, HazardEntry> blocks = new HashMap<>();
        Level level = player.level();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;

                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

                    Block block = level.getBlockState(pos).getBlock();
                    Optional<HazardEntry> optionalEntry = HazardSystem.getOptionalEntryFromBlock(block);
                    optionalEntry.ifPresent((entry) -> {
                        blocks.put(new BlockPos(pos), entry);
                    });
                }
            }
        }

        return blocks;
    }

    public static HashMap<BlockPos, AssemblyBlockEntity> findAssemblies(BlockPos center, Level level, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        HashMap<BlockPos, AssemblyBlockEntity> blocks = new HashMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;

                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

                    BlockEntity blockEntity = level.getBlockEntity(pos);

                    if (blockEntity instanceof AssemblyBlockEntity assembly) {
                        blocks.put(new BlockPos(pos), assembly);
                    }
                }
            }
        }

        return blocks;
    }

    public static ArrayList<BlockPos> getBresenhamLine(BlockPos org, BlockPos end) { // This method made me want to kill myself, Bresenham deserves a special place in hell
        ArrayList<BlockPos> blocks = new ArrayList<>();

        int x1 = org.getX(), y1 = org.getY(), z1 = org.getZ();
        int x2 = end.getX(), y2 = end.getY(), z2 = end.getZ();

        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;

        int err1, err2;

        if (dx >= dy && dx >= dz) {
            err1 = 2 * dy - dx;
            err2 = 2 * dz - dx;

            while (x1 != x2) {
                blocks.add(new BlockPos(x1, y1, z1));
                if (err1 > 0) {
                    y1 += sy;
                    err1 -= 2 * dx;
                }
                if (err2 > 0) {
                    z1 += sz;
                    err2 -= 2 * dx;
                }
                err1 += 2 * dy;
                err2 += 2 * dz;
                x1 += sx;
            }
        } else if (dy >= dx && dy >= dz) {
            err1 = 2 * dx - dy;
            err2 = 2 * dz - dy;

            while (y1 != y2) {
                blocks.add(new BlockPos(x1, y1, z1));
                if (err1 > 0) {
                    x1 += sx;
                    err1 -= 2 * dy;
                }
                if (err2 > 0) {
                    z1 += sz;
                    err2 -= 2 * dy;
                }
                err1 += 2 * dx;
                err2 += 2 * dz;
                y1 += sy;
            }
        } else {
            err1 = 2 * dy - dz;
            err2 = 2 * dx - dz;

            while (z1 != z2) {
                blocks.add(new BlockPos(x1, y1, z1));
                if (err1 > 0) {
                    y1 += sy;
                    err1 -= 2 * dz;
                }
                if (err2 > 0) {
                    x1 += sx;
                    err2 -= 2 * dz;
                }
                err1 += 2 * dy;
                err2 += 2 * dx;
                z1 += sz;
            }
        }

        blocks.add(new BlockPos(x2, y2, z2));
        return blocks;
    }
}
