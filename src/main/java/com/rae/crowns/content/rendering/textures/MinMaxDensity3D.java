package com.rae.crowns.content.rendering.textures;

import org.jetbrains.annotations.NotNull;

public class MinMaxDensity3D extends Texture3D {

    public MinMaxDensity3D(float @NotNull [] rgba, int w, int h, int d) {
        super(rgba, w, h, d, 2);
    }


}

