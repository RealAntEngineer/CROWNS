package com.rae.crowns.content.fields.util.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.CROWNS;
import com.rae.crowns.config.CROWNSConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.lwjgl.system.NonnullDefault;

import java.util.HashMap;
import java.util.Map;

@NonnullDefault
@EventBusSubscriber(value = Dist.CLIENT)
public class DebugRenderer {

    private static final     int                        RADIUS                = 8;
    private static final     int                        CACHE_PRUNE_DISTANCE  = 4;
    private static final     Map<BlockPos, AABBOutline> CACHE                 = new HashMap<>();
    private static final     int                        TICKING_SECTION_COLOR = 0x66CCFF; // light blue
    private static @Nullable BlockPos                   lastPlayerPos         = null;

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || !CROWNSConfigs.CLIENT.thermalVisualisation.get()) return;
        Font     font      = mc.font;
        Level    level     = mc.level;
        BlockPos playerPos = mc.player.blockPosition();

        pruneCacheIfPlayerMoved(playerPos);

        PoseStack poseStack = event.getPoseStack();
        float     pt        = AnimationTickHolder.getPartialTicks();

        // Render AABB for ticking sections
        poseStack.pushPose();
        renderTickingSectionsAABB(poseStack, event.getCamera().getPosition(), pt);
        poseStack.popPose();
        // Render temperature text
        renderTemperatureText(level, font, poseStack, playerPos);
        //renderVelocityVectors(level, poseStack, playerPos);

    }

    private static void pruneCacheIfPlayerMoved(BlockPos playerPos) {
        if (lastPlayerPos == null) {
            lastPlayerPos = playerPos;
            return;
        }

        if (playerPos.distManhattan(lastPlayerPos) > CACHE_PRUNE_DISTANCE) {
            lastPlayerPos = playerPos;
            CACHE.keySet().removeIf(pos -> !pos.closerThan(playerPos, RADIUS + 4));
        }
    }

    private static void renderTickingSectionsAABB(PoseStack poseStack, Vec3 cameraPos, float pt) {
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();


        for (SectionPos section : LocalPhysicData.getTickingSections()) {
            int baseX = section.x() << 4;
            int baseY = section.y() << 4;
            int baseZ = section.z() << 4;

            // One AABB per section
            BlockPos min = new BlockPos(baseX, baseY, baseZ);
            BlockPos max = new BlockPos(baseX + 16, baseY + 16, baseZ + 16);
            AABB     box = new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max));

            AABBOutline outline = CACHE.computeIfAbsent(min, p -> new AABBOutline(box));
            outline.getParams().colored(TICKING_SECTION_COLOR).lineWidth(1 / 16f);
            outline.render(poseStack, buffer, cameraPos, pt);
        }

        buffer.draw();
    }

    private static void renderTemperatureText(Level level, Font font, PoseStack poseStack, BlockPos playerPos) {
        float     threshold = CROWNSConfigs.CLIENT.visualisationThreshold.getF();
        Minecraft mc        = Minecraft.getInstance();
        Vec3      cam       = mc.gameRenderer.getMainCamera().getPosition();

        // Preallocate a mutable BlockPos and Vec3 to avoid object allocation in loops
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Vec3                     labelPos;

        int qx = QuartPos.fromBlock(playerPos.getX());
        int qy = QuartPos.fromBlock(playerPos.getY());
        int qz = QuartPos.fromBlock(playerPos.getZ());

        // Get the biome directly from the noise source
        Holder<Biome> biome = level.getBiomeManager()
                .getNoiseBiomeAtQuart(qx, qy, qz);
        MultiBufferSource.BufferSource buffer =
                mc.renderBuffers().bufferSource();
        float defaultTemp = CROWNS.BIOME_TEMPERATURES.getValue(biome.value(), 300f);
        float width = font.width(formatTemp(defaultTemp)) / 2f;

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    mutablePos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    if (!level.isLoaded(mutablePos)) continue;

                    float temp = LocalPhysicData.getTemperature(mutablePos);
                    if (Math.abs(temp - defaultTemp) < threshold) continue;

                    int color = temperatureToColor(temp);
                    labelPos = Vec3.atCenterOf(mutablePos);
                    renderFloatingText(poseStack, font, width,formatTemp(temp), labelPos, color, cam, mc, buffer);
                }
            }
        }
    }

    private static int temperatureToColor(float temperature) {
        float t = Math.clamp((temperature - 200f) / 200f, 0f, 1f);
        int   r = (int) (t * 255);
        int   g = (int) ((1 - Math.abs(t - 0.5f) * 2) * 255);
        int   b = (int) ((1 - t) * 255);
        return (0xFF << 24) | (r << 16) | (g << 8) | b; // ← added alpha
    }

    private static void renderFloatingText(PoseStack poseStack, Font font, float width, String text,
                                           Vec3 worldPos, int color, Vec3 cam, Minecraft mc,
                                           MultiBufferSource.BufferSource buffer) {
        double dx = worldPos.x - cam.x;
        double dy = worldPos.y - cam.y;
        double dz = worldPos.z - cam.z;

        // Yaw: rotate around Y so the text faces the camera in the XZ plane
        float yaw = (float) Math.atan2(dx, dz);

        // Pitch: tilt up/down to face the camera vertically
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);
        float pitch          = -(float) Math.atan2(dy, horizontalDist);

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(new Quaternionf().rotationYXZ(yaw, pitch, 0f));
        poseStack.scale(-0.02F, -0.02F, -0.02F);

        font.drawInBatch(text, -width, 0, color, false, poseStack.last().pose(),
                buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    private static String formatTemp(double temp) {
        long rounded = Math.round(temp * 10);
        int intPart = (int)(rounded / 10);
        int decDigit = (int)(rounded % 10); // always positive: 0–6000 K, no negatives

        // Determine digit count via thresholds
        int intDigits = intPart >= 1000 ? 4
                : intPart >= 100  ? 3
                  : intPart >= 10   ? 2
                    : 1;

        // Format: intDigits + '.' + 1 decimal + 'K'
        char[] buf = new char[intDigits + 3];
        int pos = buf.length - 1;

        buf[pos--] = 'K';
        buf[pos--] = (char)('0' + decDigit);
        buf[pos--] = '.';

        int n = intPart;
        int count = intDigits;
        while (count-- > 0) {
            buf[pos--] = (char)('0' + (n % 10));
            n /= 10;
        }

        return new String(buf);
    }
}