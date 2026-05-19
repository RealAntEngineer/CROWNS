package com.rae.flow.commun;

import org.joml.Vector3f;

public interface IAirflowPath {
    FlowLine getSpline();

    double getSpeedAtT(double t); // Interpolates speed at a given point along the spline

    Vector3f getColorAtT(double t); // Returns the color at a given point along the spline (RGB)
}
