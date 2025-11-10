package com.rae.crowns.mixin;

import com.rae.crowns.content.fields.advection.VelocityDataLayer;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.rae.crowns.content.fields.util.PosPackingUtil.packSection;

@Mixin(AirCurrent.class)
public class AirCurrentRansMixin {

    @Final
    @Shadow(remap = false)
    public IAirCurrentSource source;

    @Shadow(remap = false)
    public Direction direction;

    @Shadow(remap = false)
    public float maxDistance;

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    public void updateVelocityField(CallbackInfo ci) {
        Level world = source.getAirCurrentWorld();
        if (!(world instanceof ServerLevel serverLevel)) return;

        PhysicsWorldData data = PhysicsSaveManager.get(serverLevel);
        Direction dir = direction;

        // --- Only one block directly in front of the fan ---
        BlockPos targetPos = source.getAirCurrentPos().relative(dir);

        // Stop if unloaded or blocked
        if (!world.isLoaded(targetPos)) return;

        long sectionPos = packSection(
                targetPos.getX() >> 4,
                targetPos.getY() >> 4,
                targetPos.getZ() >> 4
        );

        VelocityDataLayer vxLayer = data.getLayer(DataLayerType.VX, sectionPos);
        VelocityDataLayer vyLayer = data.getLayer(DataLayerType.VY, sectionPos);
        VelocityDataLayer vzLayer = data.getLayer(DataLayerType.VZ, sectionPos);
        if (vxLayer == null || vyLayer == null || vzLayer == null) return;

        int lx = targetPos.getX() & 15;
        int ly = targetPos.getY() & 15;
        int lz = targetPos.getZ() & 15;

        // --- Compute target airflow vector ---
        // Magnitude can scale with fan speed / distance / etc.
        float targetSpeed = Math.max(0.01f, Math.min(maxDistance / source.getMaxDistance() * 20, 20.0f));
        float tx = dir.getStepX() * targetSpeed;
        float ty = dir.getStepY() * targetSpeed;
        float tz = dir.getStepZ() * targetSpeed;

        // --- Blend smoothly with current velocity ---
        float blend = 0.5f; // 0 = ignore fan, 1 = overwrite completely

        float oldVx = vxLayer.get(lx, ly, lz);
        float oldVy = vyLayer.get(lx, ly, lz);
        float oldVz = vzLayer.get(lx, ly, lz);

        float newVx = oldVx * (1 - blend) + tx * blend;
        float newVy = oldVy * (1 - blend) + ty * blend;
        float newVz = oldVz * (1 - blend) + tz * blend;

        // --- Write updated values ---
        vxLayer.set(lx, ly, lz, newVx);
        vyLayer.set(lx, ly, lz, newVy);
        vzLayer.set(lx, ly, lz, newVz);

        // --- Mark section dirty for CFD update ---
        data.setDirty(sectionPos);
    }
}