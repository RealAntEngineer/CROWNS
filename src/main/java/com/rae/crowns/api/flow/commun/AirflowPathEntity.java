package com.rae.crowns.api.flow.commun;
import com.mojang.math.Vector3f;
import com.rae.crowns.api.flow.commun.FlowLine;
import com.rae.crowns.api.flow.commun.IAirflowPath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class AirflowPathEntity extends Entity implements IAirflowPath {
    private FlowLine spline;
    private final double[] speedAtPoints;
    private final Vector3f[] colorsAtPoints;

    public AirflowPathEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.spline = null; // Initialize properly later
        this.speedAtPoints = new double[0]; // Initialize based on spline points
        this.colorsAtPoints = new Vector3f[0]; // Initialize based on spline points
    }

    public AirflowPathEntity(EntityType<?> type, Level world, FlowLine spline, double[] speedAtPoints, Vector3f[] colorsAtPoints) {
        super(type, world);
        this.spline = spline;
        this.speedAtPoints = speedAtPoints;
        this.colorsAtPoints = colorsAtPoints;
    }

    public FlowLine getSpline() {
        return spline;
    }

    public double getSpeedAtT(double t) {
        int n = speedAtPoints.length;
        int segment = Math.min((int) (t * (n - 1)), n - 2);
        double localT = (t * (n - 1)) - segment;
        return speedAtPoints[segment] * (1 - localT) + speedAtPoints[segment + 1] * localT;
    }

    public Vector3f getColorAtT(double t) {
        int n = colorsAtPoints.length;
        int segment = Math.min((int) (t * (n - 1)), n - 2);
        double localT = (t * (n - 1)) - segment;

        Vector3f startColor = colorsAtPoints[segment];
        Vector3f endColor = colorsAtPoints[segment + 1];

        float red = (float) (startColor.x() * (1 - localT) + endColor.x() * localT);
        float green = (float) (startColor.y() * (1 - localT) + endColor.y() * localT);
        float blue = (float) (startColor.z() * (1 - localT) + endColor.z() * localT);

        return new Vector3f(red, green, blue);
    }

    @Override
    protected void defineSynchedData() {
        // Entity-specific data synchronization
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // Deserialize spline, speeds, and colors from NBT
        this.spline = FlowLine.deserializeNBT(compound.getCompound("BSpline"));
        // Deserialize speedAtPoints and colorsAtPoints as well
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // Serialize spline, speeds, and colors to NBT
        compound.put("BSpline", spline.serializeNBT());
        // Add serialization for speedAtPoints and colorsAtPoints
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        // For syncing entity with client (optional)
        return null;
    }
}
