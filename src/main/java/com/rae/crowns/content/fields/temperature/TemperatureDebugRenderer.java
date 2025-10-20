package com.rae.crowns.content.fields.temperature;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.config.CROWNSConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TemperatureDebugRenderer {
    public enum RenderMode {
        AABB,
        TEXT
    }

    // 🧭 Switch this to change render mode
    private static final RenderMode RENDER_MODE = RenderMode.TEXT;
    // Smaller cubic radius for performance
    private static final int RADIUS = 8;
    private static final int INSTANCE_SIZE = 8; // subcube side length
    private static BlockPos lastPlayerPos = null;

    // Cache outlines to avoid constant reallocation
    private static final Map<BlockPos, AABBOutline> CACHE = new HashMap<>();
    private static final int CACHE_PRUNE_DISTANCE = 4;

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !CROWNSConfigs.CLIENT.thermalVisualisation.get()) return;
        BlockPos playerPos = mc.player.blockPosition();
        pruneCacheIfPlayerMoved(playerPos);


        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        float pt = AnimationTickHolder.getPartialTicks();

        // Translate to camera origin once
        poseStack.pushPose();
        if (RENDER_MODE == RenderMode.AABB) {
            //poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);


            // Split the render into small instances to prevent GPU buffer overflow
            for (int xi = -RADIUS; xi <= RADIUS; xi += INSTANCE_SIZE) {
                for (int yi = -RADIUS; yi <= RADIUS; yi += INSTANCE_SIZE) {
                    for (int zi = -RADIUS; zi <= RADIUS; zi += INSTANCE_SIZE) {
                        renderSubCube(poseStack, playerPos, cameraPos, xi, yi, zi, pt);
                    }
                }
            }
            RenderSystem.enableCull();
        } else {
            //poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Font font = mc.font;
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = -RADIUS; y <= RADIUS; y++) {
                    for (int z = -RADIUS; z <= RADIUS; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);
                        if (!mc.level.isLoaded(pos)) continue;

                        float temp = LocalTemperatureData.getTemperature(pos);
                        if (Math.abs(temp - 300f) < 1f) continue; // skip ambient temps

                        int color = temperatureToColor(temp);


                        // Render text centered slightly above block
                        Vec3 labelPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        renderFloatingText(poseStack, font, String.format("%.1fK", temp), labelPos, color, pt);
                    }
                }
            }
        }
        poseStack.popPose();
    }

    private static void renderSubCube(PoseStack poseStack, BlockPos playerPos, Vec3 cameraPos, int offsetX, int offsetY, int offsetZ, float pt) {
        Minecraft mc = Minecraft.getInstance();
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();

        int half = INSTANCE_SIZE / 2;
        assert mc.level != null;
        for (int x = offsetX - half; x < offsetX + half; x++) {
            for (int y = offsetY - half; y < offsetY + half; y++) {
                for (int z = offsetZ - half; z < offsetZ + half; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (!mc.level.isLoaded(pos)) continue;

                    float temp = LocalTemperatureData.getTemperature(pos);
                    if (Math.abs(temp - 300f) < 1f) continue; // skip near-ambient blocks

                    int color = temperatureToColor(temp);
                    AABBOutline outline = CACHE.computeIfAbsent(pos, p -> new AABBOutline(new AABB(p)));

                    outline.getParams()
                            .colored(color)
                            .lineWidth(1 / 16f);

                    outline.render(poseStack, buffer, cameraPos, pt);
                }
            }
        }

        // Flush GPU buffer after each subcube to prevent overflow
        buffer.draw();
    }

    private static void renderFloatingText(PoseStack poseStack, Font font, String text, Vec3 worldPos, int color, float pt) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = worldPos.x - cam.x;
        double dy = worldPos.y - cam.y;
        double dz = worldPos.z - cam.z;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.02F, -0.02F, 0.02F); // scale down text size

        float width = font.width(text) / 2f;
        font.drawInBatch(
                text, -width, 0, color, false,
                poseStack.last().pose(), Minecraft.getInstance().renderBuffers().bufferSource(),
                Font.DisplayMode.SEE_THROUGH, 0, 15728880
        );

        poseStack.popPose();
    }
    /**
     * Clears cached outlines if the player moved more than CACHE_PRUNE_DISTANCE blocks.
     */
    private static void pruneCacheIfPlayerMoved(BlockPos playerPos) {
        if (lastPlayerPos == null) {
            lastPlayerPos = playerPos;
            return;
        }

        if (playerPos.distManhattan(lastPlayerPos) > CACHE_PRUNE_DISTANCE) {
            lastPlayerPos = playerPos;

            // Optional: only remove entries outside radius, not everything
            Iterator<BlockPos> it = CACHE.keySet().iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                if (pos.closerThan(playerPos, RADIUS + 4)) continue; // keep nearby outlines
                it.remove();
            }
        }
    }

    private static int temperatureToColor(float temperature) {
        // 200–400K → blue–red
        float t = Math.min(1f, Math.max(0f, (temperature - 200f) / 200f));
        int r = (int) (t * 255);
        int g = (int) ((1 - Math.abs(t - 0.5f) * 2) * 255);
        int b = (int) ((1 - t) * 255);
        return (r << 16) | (g << 8) | b;
    }
}
