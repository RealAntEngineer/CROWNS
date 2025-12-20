package com.rae.crowns.content.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.rae.crowns.content.rendering.util.VolumeCubeMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public abstract class VolumeInstance {

    public Vec3 position = Vec3.ZERO;
    public Vec3 size = new Vec3(1, 1, 1);

    // ✅ Binds textures + brick uniforms
    public final void bind(ShaderInstance shader, Vec3 cameraPos, int maxSteps) {
        RenderSystem.assertOnRenderThread();

        additionalBindings(shader, cameraPos, maxSteps);

        int depthTex = Minecraft.getInstance()
                .getMainRenderTarget()
                .getDepthTextureId();

        RenderSystem.activeTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTex);
        shader.safeGetUniform("sceneDepth").set(2);

        // Volume bounds

        Vec3 worldMin = position;
        Vec3 worldMax = position.add(size);

        shader.safeGetUniform("volumeMin").set((float) worldMin.x, (float) worldMin.y, (float) worldMin.z);
        shader.safeGetUniform("volumeMax").set((float) worldMax.x, (float) worldMax.y, (float) worldMax.z);

        // Raymarch parameters
        shader.safeGetUniform("maxSteps").set(maxSteps);

        shader.safeGetUniform("skipThreshold").set(0f);
        // Camera position
        shader.safeGetUniform("cameraPos").set((float) (cameraPos.x), (float) (cameraPos.y), (float) (cameraPos.z));
    }

    public void additionalBindings(ShaderInstance shader, Vec3 cameraPos, int maxSteps) {
    }

    public final void render(PoseStack poseStack, ShaderInstance shader, Vec3 cameraPos, int maxSteps) {
        poseStack.pushPose();

        // Apply instance transform
        poseStack.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
        //poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        //poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        //poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.scale((float) size.x, (float) size.y, (float) size.z);


        Matrix4f modelViewMat = poseStack.last().pose();  // model/world transform
        //shader.safeGetUniform("ModelViewMat").set(modelViewMat);

        Matrix4f modelMat = new Matrix4f().identity();
        modelMat.translate((float) (position.x), (float) (position.y), (float) (position.z));
        modelMat.scale((float) size.x, (float) size.y, (float) size.z);
        shader.safeGetUniform("ModelMat").set(modelMat);

        // Compute inverse view: InvViewMat = inverse(ModelMat) × inverse(ModelViewMat)
        Matrix4f invViewMat = new Matrix4f(modelMat).invert();
        invViewMat.mul(new Matrix4f(modelViewMat).invert());
        shader.safeGetUniform("InvViewMat").set(invViewMat);

        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        //shader.safeGetUniform("ProjMat").set(projMat); already set by minecraft

        shader.safeGetUniform("InvProjMat").set(new Matrix4f(projMat).invert());


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
