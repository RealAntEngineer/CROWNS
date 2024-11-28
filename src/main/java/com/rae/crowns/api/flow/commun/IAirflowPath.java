package com.rae.crowns.api.flow.commun;

import com.mojang.math.Vector3f;

public interface IAirflowPath {
    FlowLine getSpline();
    double getSpeedAtT(double t); // Interpolates speed at a given point along the spline
    Vector3f getColorAtT(double t); // Returns the color at a given point along the spline (RGB)
}
