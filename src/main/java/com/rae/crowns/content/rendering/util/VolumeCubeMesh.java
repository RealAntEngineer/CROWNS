package com.rae.crowns.content.rendering.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public class VolumeCubeMesh {

    public static VertexBuffer VBO;

    public static void init() {
        System.out.println("initializing VBO for cube mesh");
        if (VBO != null) return;

        BufferBuilder builder = new BufferBuilder(256);

        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);

        float h = 1f;

        // -------- FRONT (+Z)
        builder.vertex(0, 0,  h).endVertex();
        builder.vertex( h, 0,  h).endVertex();
        builder.vertex( h,  h,  h).endVertex();

        builder.vertex(0, 0,  h).endVertex();
        builder.vertex( h,  h,  h).endVertex();
        builder.vertex(0,  h,  h).endVertex();

        // -------- BACK (-Z)
        builder.vertex( h, 0, 0).endVertex();
        builder.vertex(0, 0, 0).endVertex();
        builder.vertex(0,  h, 0).endVertex();

        builder.vertex( h, 0, 0).endVertex();
        builder.vertex(0,  h, 0).endVertex();
        builder.vertex( h,  h, 0).endVertex();

        // -------- LEFT (-X)
        builder.vertex(0, 0, 0).endVertex();
        builder.vertex(0, 0,  h).endVertex();
        builder.vertex(0,  h,  h).endVertex();

        builder.vertex(0, 0, 0).endVertex();
        builder.vertex(0,  h,  h).endVertex();
        builder.vertex(0,  h, 0).endVertex();

        // -------- RIGHT (+X)
        builder.vertex( h, 0,  h).endVertex();
        builder.vertex( h, 0, 0).endVertex();
        builder.vertex( h,  h, 0).endVertex();

        builder.vertex( h, 0,  h).endVertex();
        builder.vertex( h,  h, 0).endVertex();
        builder.vertex( h,  h,  h).endVertex();

        // -------- TOP (+Y)
        builder.vertex(0,  h,  h).endVertex();
        builder.vertex( h,  h,  h).endVertex();
        builder.vertex( h,  h, 0).endVertex();

        builder.vertex(0,  h,  h).endVertex();
        builder.vertex( h,  h, 0).endVertex();
        builder.vertex(0,  h, 0).endVertex();

        // -------- BOTTOM (-Y)
        builder.vertex(0, 0, 0).endVertex();
        builder.vertex( h, 0, 0).endVertex();
        builder.vertex( h, 0,  h).endVertex();

        builder.vertex(0, 0, 0).endVertex();
        builder.vertex( h, 0,  h).endVertex();
        builder.vertex(0, 0,  h).endVertex();

        VBO = new VertexBuffer(VertexBuffer.Usage.STATIC);
        VBO.bind();
        VBO.upload(builder.end());
        VertexBuffer.unbind();

        System.out.println("finished initializing VBO for cube mesh");
    }
}