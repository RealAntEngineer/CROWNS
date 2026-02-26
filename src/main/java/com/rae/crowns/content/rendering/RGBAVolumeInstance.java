package com.rae.crowns.content.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rae.crowns.content.rendering.textures.MinMaxDensity3D;
import com.rae.crowns.content.rendering.textures.RGBA3D;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

public class RGBAVolumeInstance extends VolumeInstance {

    // GPU textures
    public final RGBA3D volumeColors;
    public final MinMaxDensity3D bricks;

    // Volume resolution
    public final int Nx, Ny, Nz;

    // Brick resolution
    public final int Bx, By, Bz;
    public final int brickSize;

    // Transform
    public float yaw, pitch, roll;

    //scaling
    public float opacityScale = 1f;


    /**
     * Constructs a volumetric instance with its color volume and automatically computed brick min/max.
     * The brick min/max texture is computed from the alpha channel of the input RGBA array.
     *
     * @param volumeRGBA The RGBA float array for the volume data
     * @param Nx         Number of voxels in X
     * @param Ny         Number of voxels in Y
     * @param Nz         Number of voxels in Z
     * @param brickSize  Size of each brick (in voxels per dimension)
     */
    public RGBAVolumeInstance(float[] volumeRGBA, int Nx, int Ny, int Nz, int brickSize) {
        this.Nx = Nx;
        this.Ny = Ny;
        this.Nz = Nz;
        this.brickSize = brickSize;

        this.Bx = (Nx + brickSize - 1) / brickSize;
        this.By = (Ny + brickSize - 1) / brickSize;
        this.Bz = (Nz + brickSize - 1) / brickSize;

        this.volumeColors = new RGBA3D(volumeRGBA, Nx, Ny, Nz);

        // Automatically generate brick data from alpha
        float[] brickData = buildBrickMinMaxFromAlpha(volumeRGBA, Nx, Ny, Nz, brickSize);
        this.bricks = new MinMaxDensity3D(brickData, Bx, By, Bz);
    }

    /**
     * Generates the min/max alpha values for a 3D volume, subdivided into bricks.
     * Each brick contains the minimum and maximum alpha value of all voxels inside it.
     * This is useful for empty-space skipping in raymarching.
     *
     * @param rgba      The RGBA float array of the volume, linear layout: [z * Ny * Nx + y * Nx + x] * 4 + channel
     * @param Nx        Number of voxels in X dimension
     * @param Ny        Number of voxels in Y dimension
     * @param Nz        Number of voxels in Z dimension
     * @param brickSize Number of voxels per side in each brick (e.g., 8)
     * @return Float array containing min/max alpha per brick: [R=min, G=max, ...] for each brick
     */
    public static float[] buildBrickMinMaxFromAlpha(float[] rgba, int Nx, int Ny, int Nz, int brickSize) {
        int Bx = (Nx + brickSize - 1) / brickSize;
        int By = (Ny + brickSize - 1) / brickSize;
        int Bz = (Nz + brickSize - 1) / brickSize;

        float[] brickData = new float[Bx * By * Bz * 2]; // R = min, G = max

        for (int bz = 0; bz < Bz; bz++) {
            int z0 = bz * brickSize;
            int z1 = Math.min(Nz, (bz + 1) * brickSize);

            for (int by = 0; by < By; by++) {
                int y0 = by * brickSize;
                int y1 = Math.min(Ny, (by + 1) * brickSize);

                for (int bx = 0; bx < Bx; bx++) {
                    int x0 = bx * brickSize;
                    int x1 = Math.min(Nx, (bx + 1) * brickSize);

                    float minA = Float.POSITIVE_INFINITY;
                    float maxA = Float.NEGATIVE_INFINITY;

                    for (int z = z0; z < z1; z++) {
                        for (int y = y0; y < y1; y++) {
                            for (int x = x0; x < x1; x++) {
                                int idx = (z * Ny * Nx + y * Nx + x) * 4 + 3; // alpha channel
                                float a = rgba[idx];
                                if (a < minA) minA = a;
                                if (a > maxA) maxA = a;
                            }
                        }
                    }

                    if (minA == Float.POSITIVE_INFINITY) minA = 0f;
                    if (maxA == Float.NEGATIVE_INFINITY) maxA = 0f;

                    int brickIdx = bx + by * Bx + bz * (Bx * By);
                    brickData[brickIdx * 2] = minA;
                    brickData[brickIdx * 2 + 1] = maxA;
                }
            }
        }

        return brickData;
    }

    public void additionalBindings(ShaderInstance shader, Vec3 cameraPos) {
        // Volume → unit 1
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, volumeColors.texId);
        shader.setSampler("colorVolume", 0);//volumeColors.texId);

        // Brick → unit 1
        /*RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, bricks.texId);
        shader.setSampler("brickMinMax", bricks.texId);*/

        // Brick parameters
        shader.safeGetUniform("bricksCount").set(Bx, By, Bz);
        shader.safeGetUniform("volumesCount").set(Nx, Ny, Nz);
        shader.safeGetUniform("opacity").set(opacityScale);
    }

}

