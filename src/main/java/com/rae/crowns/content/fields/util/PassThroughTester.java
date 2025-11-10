package com.rae.crowns.content.fields.util;

import com.simibubi.create.AllTags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PassThroughTester {

    //this is the code from the

    private static final double[][] DEPTH_TEST_COORDINATES = {
            {0.25, 0.25},
            {0.25, 0.75},
            {0.5, 0.5},
            {0.75, 0.25},
            {0.75, 0.75}
    };

    // Finds the maximum depth of the shape when traveling in the given direction.
    // The result is always positive.
    // If there is a hole, the result will be Double.POSITIVE_INFINITY.
    static double findMaxDepth(VoxelShape shape, Direction direction) {
        Direction.Axis axis = direction.getAxis();
        Direction.AxisDirection axisDirection = direction.getAxisDirection();
        double maxDepth = 0;

        for (double[] coordinates : DEPTH_TEST_COORDINATES) {
            double depth;
            if (axisDirection == Direction.AxisDirection.POSITIVE) {
                double min = shape.min(axis, coordinates[0], coordinates[1]);
                if (min == Double.POSITIVE_INFINITY) {
                    return Double.POSITIVE_INFINITY;
                }
                depth = min;
            } else {
                double max = shape.max(axis, coordinates[0], coordinates[1]);
                if (max == Double.NEGATIVE_INFINITY) {
                    return Double.POSITIVE_INFINITY;
                }
                depth = 1 - max;
            }

            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }

        return maxDepth;
    }

    static boolean shouldAlwaysPass(BlockState state) {
        return AllTags.AllBlockTags.FAN_TRANSPARENT.matches(state);
    }
}
