package com.rae.crowns.content.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

public class VolumeInstance {

    // ✅ GPU textures
    public final VolumeTexture3D volumeColors;
    public final BrickTexture3D bricks;

    // ✅ Volume resolution
    public final int Nx, Ny, Nz;

    // ✅ Brick resolution
    public final int Bx, By, Bz;
    public final int brickSize;

    // ✅ Transform
    public Vec3 position = Vec3.ZERO;
    public float yaw, pitch, roll;
    public Vec3 size = new Vec3(1, 1, 1);

    /**
     * Constructs a volumetric instance with its color volume and automatically computed brick min/max.
     * The brick min/max texture is computed from the alpha channel of the input RGBA array.
     *
     * @param volumeRGBA  The RGBA float array for the volume data
     * @param Nx          Number of voxels in X
     * @param Ny          Number of voxels in Y
     * @param Nz          Number of voxels in Z
     * @param brickSize   Size of each brick (in voxels per dimension)
     */
    public VolumeInstance(float[] volumeRGBA, int Nx, int Ny, int Nz, int brickSize) {
        this.Nx = Nx;
        this.Ny = Ny;
        this.Nz = Nz;
        this.brickSize = brickSize;

        this.Bx = (Nx + brickSize - 1) / brickSize;
        this.By = (Ny + brickSize - 1) / brickSize;
        this.Bz = (Nz + brickSize - 1) / brickSize;

        this.volumeColors = new VolumeTexture3D(volumeRGBA, Nx, Ny, Nz);

        // Automatically generate brick data from alpha
        float[] brickData = buildBrickMinMaxFromAlpha(volumeRGBA, Nx, Ny, Nz, brickSize);
        this.bricks = new BrickTexture3D(brickData, Bx, By, Bz);
    }

    /**
     * Generates the min/max alpha values for a 3D volume, subdivided into bricks.
     * Each brick contains the minimum and maximum alpha value of all voxels inside it.
     * This is useful for empty-space skipping in raymarching.
     *
     * @param rgba       The RGBA float array of the volume, linear layout: [z * Ny * Nx + y * Nx + x] * 4 + channel
     * @param Nx         Number of voxels in X dimension
     * @param Ny         Number of voxels in Y dimension
     * @param Nz         Number of voxels in Z dimension
     * @param brickSize  Number of voxels per side in each brick (e.g., 8)
     * @return           Float array containing min/max alpha per brick: [R=min, G=max, ...] for each brick
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


    // ✅ Binds textures + brick uniforms
    public void bind(ShaderInstance shader, Vec3 cameraPos, int maxSteps) {
        RenderSystem.assertOnRenderThread();

        // Volume → unit 0
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, volumeColors.texId);
        shader.safeGetUniform("colorVolume").set(0);

        // Brick → unit 1
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, bricks.texId);
        shader.safeGetUniform("brickMinMax").set(1);

        // Brick parameters
        shader.safeGetUniform("bricksCount").set(Bx, By, Bz);
        shader.safeGetUniform("volumesCount").set(Nx, Ny, Nz);

        // Volume bounds

        Vec3 worldMin = position;
        Vec3 worldMax = position.add(size);

        shader.safeGetUniform("volumeMin").set((float) worldMin.x, (float) worldMin.y, (float) worldMin.z);
        shader.safeGetUniform("volumeMax").set((float) worldMax.x, (float) worldMax.y, (float) worldMax.z);

        // If you want a tighter box inside the volume, set boxMin and boxMax
        //shader.safeGetUniform("boxMin").set((float) worldMin.x, (float) worldMin.y, (float) worldMin.z);
        //shader.safeGetUniform("boxMax").set((float) worldMax.x, (float) worldMax.y, (float) worldMax.z);


        // Raymarch parameters
        shader.safeGetUniform("maxSteps").set(maxSteps);
        //shader.safeGetUniform("stepScale").set(stepScale);
        shader.safeGetUniform("skipThreshold").set(0f);
        // Camera position
        shader.safeGetUniform("cameraPos").set((float) (cameraPos.x), (float) (cameraPos.y), (float) (cameraPos.z));
    }


    public void render(PoseStack poseStack, ShaderInstance shader, Vec3 cameraPos, int maxSteps) {
        poseStack.pushPose();

        // Apply instance transform
        poseStack.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
        //poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        //poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        //poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.scale((float) size.x, (float) size.y, (float) size.z);


        Matrix4f modelViewMat = poseStack.last().pose();  // model/world transform
        shader.safeGetUniform("ModelViewMat").set(modelViewMat);

        Matrix4f modelMat = new Matrix4f().identity();
        modelMat.translate((float) (position.x), (float) (position.y), (float) (position.z));
        modelMat.scale((float) size.x, (float) size.y, (float) size.z);
        shader.safeGetUniform("ModelMat").set(modelMat);

        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        shader.safeGetUniform("ProjMat").set(projMat);

        // Bind textures and uniforms
        bind(shader, cameraPos, maxSteps);

        // Draw the unit cube (surface only)
        //RenderSystem.getModelViewMatrix().set(mat);  // poseStack last pose
        //RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
        VolumeCubeMesh.VBO.bind();
        VolumeCubeMesh.VBO.drawWithShader(modelViewMat, projMat, shader);
        VertexBuffer.unbind();


        poseStack.popPose();
    }

}

