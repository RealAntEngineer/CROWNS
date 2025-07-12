package com.rae.crowns.content.nuclear;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NuclearExplosion extends Explosion {
    float radius;
    Level level;
    public NuclearExplosion(Level level, Entity source, double x, double y, double z, float radius, BlockInteraction interaction) {
        super(level, source, null, null, x, y, z, radius, false, interaction);
        this.radius = radius;
        this.level = level;
    }

    @Override
    public void explode() {
        // Skip vanilla raytrace calculation
        // Instead, directly destroy blocks in a spherical radius

        int r = Math.round(this.radius);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq <= radius * radius) {
                        mutablePos.set( getPosition().x() + dx, getPosition().y() + dy, getPosition().z() + dz);
                        if (level.isLoaded(mutablePos)) {
                            BlockState state = level.getBlockState(mutablePos);
                            if (!state.isAir()) {
                                level.destroyBlock(mutablePos, true); // true for drops
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void finalizeExplosion(boolean spawnParticles) {
        // Skip vanilla finalize logic if you don’t want extra effects
        // Or call super.finalizeExplosion(spawnParticles) if you want them
    }
}