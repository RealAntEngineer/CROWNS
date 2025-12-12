package com.rae.crowns.content.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_3D;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;

public class VolumeTexture3D {

    public final int texId;
    public final int width, height, depth;
    public final FloatBuffer cpuBuffer;

    public VolumeTexture3D(float[] rgba, int w, int h, int d) {
        this.width = w;
        this.height = h;
        this.depth = d;

        this.cpuBuffer = memAllocFloat(w * h * d * 4);
        this.cpuBuffer.put(rgba).flip();

        RenderSystem.assertOnRenderThread();

        this.texId = GlStateManager._genTexture();
        glBindTexture(GL_TEXTURE_3D, texId);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);

        GL12.glTexImage3D(
                GL12.GL_TEXTURE_3D,
                0,
                GL30.GL_RGBA32F,
                w, h, d,
                0,
                GL11.GL_RGBA,
                GL11.GL_FLOAT,
                cpuBuffer
        );
    }

    // ✅ FAST UPDATE WITHOUT REALLOCATION
    public void reupload() {
        RenderSystem.assertOnRenderThread();
        glBindTexture(GL_TEXTURE_3D, texId);

        GL12.glTexSubImage3D(
                GL12.GL_TEXTURE_3D,
                0,
                0, 0, 0,
                width, height, depth,
                GL11.GL_RGBA,
                GL11.GL_FLOAT,
                cpuBuffer
        );
    }
}

