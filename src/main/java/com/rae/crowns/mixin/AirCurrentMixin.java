package com.rae.crowns.mixin;

import com.rae.crowns.content.fields.temperature.ConductionDataLayer;
import com.rae.crowns.content.fields.temperature.TemperatureDataLayer;
import com.rae.crowns.content.fields.util.DataLayerType;
import com.rae.crowns.content.fields.util.PhysicsSaveManager;
import com.rae.crowns.content.fields.util.PhysicsWorldData;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AirCurrent.class)
public class AirCurrentMixin {

    @Final
    @Shadow(remap = false)
    public IAirCurrentSource source;

    @Shadow(remap = false)
    public Direction direction;

    @Shadow(remap = false)
    public float maxDistance;

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    public void thermalTransport(CallbackInfo ci) {
        Level world = source.getAirCurrentWorld();
        if (!(world instanceof ServerLevel serverLevel)) return;

        PhysicsWorldData data = PhysicsSaveManager.get(serverLevel);
        if (data == null) return;
        Direction dir = direction;

        // --- Key fix: reverse the starting point ---
        // The air is flowing from the fan outward, so if you're seeing it reversed,
        // start one block behind instead of ahead.
        BlockPos start = source.getAirCurrentPos().relative(dir.getOpposite());

        // Orthogonal directions for conduction
        Direction.Axis axis = dir.getAxis();
        Direction[] orthogonalDirs = switch (axis) {
            case X -> new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
            case Y -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            case Z -> new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
        };

        BlockPos             fanPos       = source.getAirCurrentPos(); // fan block itself
        SectionPos           startSection = SectionPos.of(fanPos);
        TemperatureDataLayer sTempLayer   = (TemperatureDataLayer) data.getLayer(startSection.asLong(),DataLayerType.TEMPERATURE);
        if (sTempLayer == null) return;

        double streamTemp = sTempLayer.get((short) (fanPos.getX() & 15), (short) (fanPos.getY() & 15), (short) (fanPos.getZ() & 15));

        // --- Move along airflow ---
        for (int i = 0; i < Math.ceil(maxDistance); i++) {
            BlockPos pos = start.relative(dir, i);

            if (!world.isLoaded(pos)) break;

            SectionPos           section   = SectionPos.of(pos);
            TemperatureDataLayer tempLayer = (TemperatureDataLayer) data.getLayer(startSection.asLong(), DataLayerType.TEMPERATURE);
            ConductionDataLayer  condLayer = (ConductionDataLayer) data.getLayer(startSection.asLong(), DataLayerType.CONDUCTION);
            if (tempLayer == null || condLayer == null) continue;

            int    rx       = pos.getX() & 15;
            int    ry       = pos.getY() & 15;
            int    rz       = pos.getZ() & 15;
            double tempHere = tempLayer.get((short) rx, (short) ry, (short) rz);
            double condHere = condLayer.get((short) rx, (short) ry, (short) rz);

            // --- Conduction with orthogonal neighbors ---
            double totalFlux = 0;
            double capacity  = 3e5;
            for (Direction ortho : orthogonalDirs) {
                BlockPos neighbor = pos.relative(ortho);
                if (!world.isLoaded(neighbor)) continue;

                SectionPos           nSection   = SectionPos.of(neighbor);
                TemperatureDataLayer nTempLayer = (TemperatureDataLayer) data.getLayer(startSection.asLong(), DataLayerType.TEMPERATURE);
                ConductionDataLayer  nCondLayer = (ConductionDataLayer) data.getLayer(startSection.asLong(), DataLayerType.CONDUCTION);
                if (nTempLayer == null || nCondLayer == null) continue;

                int    nrx          = neighbor.getX() & 15;
                int    nry          = neighbor.getY() & 15;
                int    nrz          = neighbor.getZ() & 15;
                double tempNeighbor = nTempLayer.get((short) nrx, (short) nry, (short) nrz);
                double condNeighbor = nCondLayer.get((short) nrx, (short) nry, (short) nrz);

                double delta = tempNeighbor - tempHere;
                totalFlux += delta * condNeighbor;
                nTempLayer.set((short) nrx, (short) nry, (short) nrz, (float) (tempNeighbor - delta * condNeighbor / 20 / capacity));
            }

            // --- Convection (move along the current) ---
            if (!world.getBlockState(pos).isAir()) {
                totalFlux += condHere * (tempHere - streamTemp);
            }
            streamTemp += totalFlux / 20 / capacity;

            if (world.getBlockState(pos).isAir()) {
                tempLayer.set((short) rx, (short) ry, (short) rz, (float) streamTemp);
            } else {
                tempLayer.set((short) rx, (short) ry, (short) rz, (float) (tempHere - (tempHere - streamTemp) / 20 / capacity));
            }
        }
    }
}