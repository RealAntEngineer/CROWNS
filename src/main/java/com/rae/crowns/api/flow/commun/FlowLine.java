package com.rae.crowns.api.flow.commun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FlowLine {//this is a spline
    public static final Codec<FlowLine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3.CODEC.listOf().fieldOf("controlPoints").forGetter(FlowLine::getControlPoints),  // Control points
            Codec.DOUBLE.listOf().fieldOf("speedAtPoints").forGetter(FlowLine::getSpeedAtPoints), // Speed at points
            Codec.INT.xmap(Color::new
                    ,Color::getRGB
            ).listOf().fieldOf("colorsAtPoints").forGetter(FlowLine::getColorsAtPoints) // Colors at points
    ).apply(instance, FlowLine::new));
    private final List<Vec3> controlPoints;
    private final List<Vec3> computedTangents;
    private final List<Vec3[]> coefficients;  // List of Vec3[] for the coefficients (a, b, c, d for each segment)
    private final List<Double> speedAtPoints;
    private final List<Color> colorsAtPoints;

    public FlowLine(List<Vec3> controlPoints, List<Double> speedAtPoints, List<Color> colorsAtPoints) {
        this.controlPoints = controlPoints;
        this.speedAtPoints = speedAtPoints;
        this.colorsAtPoints = colorsAtPoints;
        if (controlPoints.size() >1) {
            this.computedTangents = new ArrayList<>();
            this.coefficients = new ArrayList<>();
            computeTangents();
            computeCoefficients();

        }
        else {
            throw new IllegalArgumentException("At least two control points are required.");
        }
    }
    private void computeTangents() {

        int n = controlPoints.size();

        // Add the start tangent
        computedTangents.add(computeTangent(0,1));

        // Compute tangents for intermediate points
        for (int i = 1; i < n - 1; i++) {
            Vec3 tangent = computeTangent(i - 1, i,i + 1);
            computedTangents.add(tangent);
        }

        // Add the end tangent
        computedTangents.add(computeTangent(n-2,n-1));
    }
    public List<Vec3> getControlPoints() {
        return controlPoints;
    }

    public List<Double> getSpeedAtPoints() {
        return speedAtPoints;
    }

    public List<Color> getColorsAtPoints() {
        return colorsAtPoints;
    }

    // Get the speed at a parameter value 't' along the spline
    public double getSpeedAtT(double t) {
        int n = speedAtPoints.size();
        if (n < 2){
            return speedAtPoints.get(0);
        }
        int segment = Math.min((int) (t * (n - 1)), n - 2);
        double localT = (t * (n - 1)) - segment;
        return speedAtPoints.get(segment) * (1 - localT) + speedAtPoints.get(segment + 1) * localT;
    }

    // Get the color at a parameter value 't' along the spline
    public Color getColorAtT(double t) {
        int n = colorsAtPoints.size();
        if (n < 2){
            return colorsAtPoints.get(0);
        }
        int segment = Math.min((int) (t * (n - 1)), n - 2);
        double localT = (t * (n - 1)) - segment;

        Color startColor = colorsAtPoints.get(segment);
        Color endColor = colorsAtPoints.get(segment + 1);

        return Color.mixColors(startColor,endColor, (float)localT);
    }

    // Serialization: Write BSpline data into NBT
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Serialize control points
        ListTag pointsTag = new ListTag();
        for (Vec3 point : controlPoints) {
            CompoundTag pointTag = new CompoundTag();
            pointTag.putDouble("x", point.x());
            pointTag.putDouble("y", point.y());
            pointTag.putDouble("z", point.z());
            pointsTag.add(pointTag);
        }
        tag.put("ControlPoints", pointsTag);

        // Serialize speed at points
        ListTag speedsTag = new ListTag();
        for (double speed : speedAtPoints) {
            CompoundTag speedTag = new CompoundTag();
            speedTag.putDouble("Speed", speed);
            speedsTag.add(speedTag);
        }
        tag.put("Speeds", speedsTag);

        // Serialize colors at points
        ListTag colorsTag = new ListTag();
        for (Color color : colorsAtPoints) {
            CompoundTag colorTag = new CompoundTag();
            colorTag.putFloat("RGBA", color.getRGB());
            colorsTag.add(colorTag);
        }
        tag.put("Colors", colorsTag);

        return tag;
    }

    // Deserialization: Read BSpline data from NBT
    public static FlowLine deserializeNBT(CompoundTag tag) {
        List<Vec3> controlPoints = new ArrayList<>();

        // Deserialize control points
        ListTag pointsTag = tag.getList("ControlPoints", 10); // 10 for CompoundTag type
        for (int i = 0; i < pointsTag.size(); i++) {
            CompoundTag pointTag = pointsTag.getCompound(i);
            double x = pointTag.getDouble("x");
            double y = pointTag.getDouble("y");
            double z = pointTag.getDouble("z");
            controlPoints.add(new Vec3(x, y, z));
        }

        // Deserialize speed at points
        ListTag speedsTag = tag.getList("Speeds", 10);
        ArrayList<Double> speedAtPoints = new ArrayList<>();
        for (int i = 0; i < speedsTag.size(); i++) {
            CompoundTag speedTag = speedsTag.getCompound(i);
            speedAtPoints.set(i, speedTag.getDouble("Speed"));
        }

        // Deserialize colors at points
        ListTag colorsTag = tag.getList("Colors", 10);
        ArrayList<Color> colorsAtPoints = new ArrayList<>();
        for (int i = 0; i < colorsTag.size(); i++) {
            CompoundTag colorTag = colorsTag.getCompound(i);
            long rgba = colorTag.getLong("RGBA");
            colorsAtPoints.set(i, new Color((int) rgba));
        }

        return new FlowLine(controlPoints, speedAtPoints, colorsAtPoints);
    }

    private void computeCoefficients() {
        int n = controlPoints.size();
        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = controlPoints.get(i);
            Vec3 p1 = controlPoints.get(i + 1);

            Vec3 m0 = computedTangents.get(i);
            Vec3 m1 = computedTangents.get(i + 1);

            // Compute the cubic polynomial coefficients (a, b, c, d) for this segment
            Vec3[] coeffs = computeCoefficients(p0, p1, m0, m1);
            coefficients.add(coeffs);
        }
    }

    private Vec3[] computeCoefficients(Vec3 p0, Vec3 p1, Vec3 m0, Vec3 m1) {
        Vec3 a = p0.scale(2).subtract(p1.scale(2)).add(m0).add(m1);
        Vec3 b = p0.scale(-3).add(p1.scale(3)).subtract(m0.scale(2)).subtract(m1);

        return new Vec3[]{a, b, m0, p0};  // a, b, c, d as Vec3
    }

    public Vec3 getPoint(float t) {
        int n = controlPoints.size();
        if (n < 2) {
            throw new IllegalArgumentException("At least two control points are required.");
        }

        // Ensure t is within the range [0, 1]
        t = Math.max(0, Math.min(t, 1));

        // Scale t to the range [0, n-1] to find the segment
        float scaledT = t * (n - 1);
        int i = (int) Math.floor(scaledT);
        float localT = scaledT - i;

        // Ensure segment is within bounds
        if (i >= n - 1) {
            i = n - 2;
            localT = 1.0f;
        }

        // Retrieve pre-computed coefficients (a, b, c, d) for this segment
        Vec3[] coeffs = coefficients.get(i);
        Vec3 a = coeffs[0];
        Vec3 b = coeffs[1];
        Vec3 c = coeffs[2];
        Vec3 d = coeffs[3];
        //there is a possible bug if the points are seperated by more than 1
        return evaluateCubic(a, b, c, d, localT);
    }

    private Vec3 evaluateCubic(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        // Cubic polynomial evaluation: a * t^3 + b * t^2 + c * t + d
        return a.scale(t3).add(b.scale(t2)).add(c.scale(t)).add(d);
    }

    private Vec3 computeTangent(int index0, int index1) {
        Vec3 p0 = controlPoints.get(index0);
        Vec3 p1 = controlPoints.get(index1);
        return p1.subtract(p0);  // Example of tangent computation
    }
    private Vec3 computeTangent(int index0, int index1, int index2) {
        return computeTangent(index0, index1).add(computeTangent(index1,index2)).scale(0.5f);  // Example of tangent computation
    }
    // Network buffer serialization
    public void writeToBuffer(FriendlyByteBuf buffer) {
        CompoundTag nbt = this.serializeNBT();
        buffer.writeNbt(nbt); // Writing NBT to the buffer
    }

    public static FlowLine readFromBuffer(FriendlyByteBuf buffer) {
        CompoundTag nbt = buffer.readNbt(); // Reading NBT from the buffer
        assert nbt != null;
        return FlowLine.deserializeNBT(nbt);
    }

    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {

    }
}
