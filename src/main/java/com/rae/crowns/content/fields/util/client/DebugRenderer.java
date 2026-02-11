package com.rae.crowns.content.fields.util.client;

import com.mojang.blaze3d.vertex.*;
import com.rae.crowns.CROWNS;
import com.rae.crowns.config.CROWNSConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DebugRenderer {

    private static final int RADIUS = 8;
    private static final int CACHE_PRUNE_DISTANCE = 4;
    private static final Map<BlockPos, AABBOutline> CACHE = new HashMap<>();
    private static @Nullable BlockPos lastPlayerPos = null;

    @SubscribeEvent
    public static void onRenderWorld(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || !CROWNSConfigs.CLIENT.thermalVisualisation.get()) return;
        Font font = mc.font;
        Level level = mc.level;
        BlockPos playerPos = mc.player.blockPosition();

        pruneCacheIfPlayerMoved(playerPos);

        PoseStack poseStack = event.getPoseStack();
        float pt = AnimationTickHolder.getPartialTicks();

        // Render AABB for ticking sections
        poseStack.pushPose();
        renderTickingSectionsAABB(poseStack, event.getCamera().getPosition(), pt);
        poseStack.popPose();
        // Render temperature text
        renderTemperatureText(level, font, poseStack, playerPos);
        //renderVelocityVectors(level, poseStack, playerPos);

    }
    private static final int TICKING_SECTION_COLOR = 0x66CCFF; // light blue

    private static void renderTickingSectionsAABB(@NotNull PoseStack poseStack, @NotNull Vec3 cameraPos, float pt) {
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();


        for (SectionPos section : LocalPhysicData.getTickingSections()) {
            int baseX = section.x() << 4;
            int baseY = section.y() << 4;
            int baseZ = section.z() << 4;

            // One AABB per section
            BlockPos min = new BlockPos(baseX, baseY, baseZ);
            BlockPos max = new BlockPos(baseX + 16, baseY + 16, baseZ + 16);
            AABB box = new AABB(min, max);

            AABBOutline outline = CACHE.computeIfAbsent(min, p -> new AABBOutline(box));
            outline.getParams().colored(TICKING_SECTION_COLOR).lineWidth(1 / 16f);
            outline.render(poseStack, buffer, cameraPos, pt);
        }

        buffer.draw();
    }

    private static void renderTemperatureText(@NotNull Level level, @NotNull Font font, @NotNull PoseStack poseStack, @NotNull BlockPos playerPos) {
        float threshold = CROWNSConfigs.CLIENT.visualisationThreshold.getF();
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        // Preallocate a mutable BlockPos and Vec3 to avoid object allocation in loops
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Vec3 labelPos;

        int qx = QuartPos.fromBlock(playerPos.getX());
        int qy = QuartPos.fromBlock(playerPos.getY());
        int qz = QuartPos.fromBlock(playerPos.getZ());

        // Get the biome directly from the noise source
        Holder<Biome> biome = level.getBiomeManager()
                .getNoiseBiomeAtQuart(qx, qy, qz);
        float defaultTemp =  CROWNS.BIOME_TEMPERATURES.getValue(biome.value(), 300f);

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    mutablePos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    if (!level.isLoaded(mutablePos)) continue;

                    float temp = LocalPhysicData.getTemperature(mutablePos);
                    if (Math.abs(temp - defaultTemp) < threshold) continue;

                    int color = temperatureToColor(temp);
                    labelPos = Vec3.atCenterOf(mutablePos);
                    renderFloatingText(poseStack, font, String.format("%.1fK", temp), labelPos, color, cam, mc);
                }
            }
        }
    }

    private static void renderFloatingText(@NotNull PoseStack poseStack, @NotNull Font font, @NotNull String text, @NotNull Vec3 worldPos, int color, @NotNull Vec3 cam, @NotNull Minecraft mc) {
        double dx = worldPos.x - cam.x;
        double dy = worldPos.y - cam.y;
        double dz = worldPos.z - cam.z;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.02F, -0.02F, 0.02F);

        float width = font.width(text) / 2f;
        font.drawInBatch(
                text, -width, 0, color, false,
                poseStack.last().pose(), mc.renderBuffers().bufferSource(),
                Font.DisplayMode.SEE_THROUGH, 0, 15728880
        );
        poseStack.popPose();
    }

    private static void pruneCacheIfPlayerMoved(@NotNull BlockPos playerPos) {
        if (lastPlayerPos == null) {
            lastPlayerPos = playerPos;
            return;
        }

        if (playerPos.distManhattan(lastPlayerPos) > CACHE_PRUNE_DISTANCE) {
            lastPlayerPos = playerPos;
            CACHE.keySet().removeIf(pos -> !pos.closerThan(playerPos, RADIUS + 4));
        }
    }

    private static int temperatureToColor(float temperature) {
        float t = Math.min(1f, Math.max(0f, (temperature - 200f) / 200f));
        int r = (int) (t * 255);
        int g = (int) ((1 - Math.abs(t - 0.5f) * 2) * 255);
        int b = (int) ((1 - t) * 255);
        return (r << 16) | (g << 8) | b;
    }
}