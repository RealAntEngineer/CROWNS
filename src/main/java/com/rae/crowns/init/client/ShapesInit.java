package com.rae.crowns.init.client;

import com.simibubi.create.AllShapes;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ShapesInit {


    public static final VoxelShaper
            TURBINE = shape(makeTurbineshape())
            .forDirectional();


    public static @NotNull VoxelShape makeTurbineshape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0.0625, 0.9375, 1, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.9375, 0, -0.4375, 2, 1, 1.4375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-1, 0, -0.4375, -0.9375, 1, 1.4375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.4375, 0, -1, 1.4375, 1, -0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.4375, 0, 1.9375, 1.4375, 1, 2), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.375, 0, 1.625, 1.75, 1, 2), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.625, 0, 1.375, 2, 1, 1.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-1, 0, 1.375, -0.625, 1, 1.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.75, 0, 1.625, -0.375, 1, 2), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-1, 0, -0.75, -0.625, 1, -0.375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.75, 0, -1, -0.375, 1, -0.625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.625, 0, -0.75, 2, 1, -0.375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.375, 0, -1, 1.75, 1, -0.625), BooleanOp.OR);

        return shape;
    }

    private static AllShapes.@NotNull Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
        return shape(cuboid(x1, y1, z1, x2, y2, z2));
    }

    private static AllShapes.@NotNull Builder shape(VoxelShape shape) {
        return new AllShapes.Builder(shape);
    }

    private static @NotNull VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

}
