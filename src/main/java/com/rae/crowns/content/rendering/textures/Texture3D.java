package com.rae.crowns.content.rendering.textures;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rae.crowns.content.rendering.util.GLGuard;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_3D;

public abstract class Texture3D {

    public final int texId;
    private final int width;
    private final int height;
    private final int depth;
    private final int glPixelFormat;
    private final @NotNull FloatBuffer cpuBuffer;

    public Texture3D(float @NotNull [] buffer, int width, int height, int depth, int dataSize) {
        synchronized (GLGuard.GL_LOCK) {//we are accessing raw GL parameters so we need to protect it.
            //GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            //System.out.println("sending a 3d texture");

            this.width = width;
            this.height = height;
            this.depth = depth;

            this.cpuBuffer = BufferUtils.createFloatBuffer(width * height * depth * dataSize);
            this.cpuBuffer.put(buffer).flip();

            RenderSystem.assertOnRenderThread();

            this.texId = GlStateManager._genTexture();
            glBindTexture(GL_TEXTURE_3D, texId);

            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);

            this.glPixelFormat = switch (dataSize) {
                case 1 -> GL11.GL_RED;
                case 2 -> GL30.GL_RG;
                case 3 -> GL11.GL_RGB;
                case 4 -> GL11.GL_RGBA;
                default -> throw new IllegalStateException("Invalid data size: " + dataSize);
            };

            int glImageFormat = switch (dataSize) {
                case 1 -> GL30.GL_R32F;
                case 2 -> GL30.GL_RG32F;
                case 3 -> GL30.GL_RGB32F;
                case 4 -> GL30.GL_RGBA32F;
                default -> throw new IllegalStateException("Invalid data size: " + dataSize);
            };

            GL12.glTexImage3D(
                    GL12.GL_TEXTURE_3D,
                    0,
                    glImageFormat,
                    width, height, depth,
                    0,
                    glPixelFormat,
                    GL11.GL_FLOAT,
                    cpuBuffer
            );
        }
    }

    // ✅ FAST UPDATE WITHOUT REALLOCATION
    public void reupload() {
        synchronized (GLGuard.GL_LOCK) {

            RenderSystem.assertOnRenderThread();
            glBindTexture(GL_TEXTURE_3D, texId);

            GL12.glTexSubImage3D(
                    GL12.GL_TEXTURE_3D,
                    0,
                    0, 0, 0,
                    width, height, depth,
                    glPixelFormat,
                    GL11.GL_FLOAT,
                    cpuBuffer
            );
        }
    }
}
