package com.rae.crowns.content.event;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.content.hazards.HazardSystem;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.rendering.VolumeWorldRenderer;
import com.rae.crowns.content.rendering.overlays.GuiOverlays;
import com.rae.crowns.content.rendering.util.SceneDepth;
import com.rae.crowns.content.sound.CrownsSoundScapes;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

import static com.rae.crowns.init.client.ShaderInit.volumeShader;
import static net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onTick(TickEvent.@NotNull ClientTickEvent event) {
        if (!isGameActive())
            return;

        Level world = Minecraft.getInstance().level;
        assert world != null;
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        CrownsSoundScapes.tick();
        SteamFlowManager.tick(world);
    }

    protected static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }

    @SubscribeEvent
    public static void renderGuiOverlays(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();

        // Render once per frame
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        GuiOverlays.dosimeterGuiOverlay(guiGraphics, mc);
    }

    @SubscribeEvent
    public static void captureSolidDepth(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();

        RenderSystem.assertOnRenderThread();

        // Ensure our depth texture matches screen size
        SceneDepth.resize(main.width, main.height);

        // Bind main framebuffer for reading
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);

        // Copy depth buffer into our standalone texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, SceneDepth.depthTexture);

        GL11.glCopyTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,              // mip level
                0, 0,           // texture offset
                0, 0,           // framebuffer offset
                main.width,
                main.height
        );

        // Cleanup
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);

        // IMPORTANT:
        // Do NOT bind this texture as a framebuffer attachment anywhere else.
    }

    /**
     * Render every frame
     */
    @SubscribeEvent
    public static void render(@NotNull RenderLevelStageEvent event) {

        if (volumeShader == null || event.getStage() != AFTER_PARTICLES) return;

        PoseStack poseStack = event.getPoseStack();
        SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();

        Vec3 cameraPos = event.getCamera().getPosition();

        VolumeWorldRenderer.render(poseStack, buffers, volumeShader, cameraPos);

        buffers.draw();

    }

    @SubscribeEvent
    public static void addToItemTooltip(@NotNull ItemTooltipEvent event) {
        if (event.getEntity() == null)
            return;

        ItemStack itemStack = event.getItemStack();
        List<Component> components = event.getToolTip();
        CompoundTag composition = itemStack.getTagElement("composition");
        if (composition != null) {
            components.add(Component.literal("composition").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
            for (ResourceLocation resourceLocation : IAmFissileMaterial.fissileCrossSection.keySet()) {
                if (composition.contains(resourceLocation.toString())) {
                    float concentration = composition.getFloat(resourceLocation.toString());
                    components.add(
                            Component.translatable(resourceLocation.toLanguageKey("nucleus")).withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(String.format(" : %.2f %%", concentration * 100)).withStyle(ChatFormatting.GRAY)));
                }
            }
        }

        // Hazard tooltips

        List<String> hazardStrings = new ArrayList<>();
        HazardSystem.addFullTooltip(itemStack, event.getEntity(), hazardStrings);

        for (String line : hazardStrings) {
            components.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }

}
