package com.rae.crowns.content.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.content.nuclear.IAmFissileMaterial;
import com.rae.crowns.content.rendering.VolumeWorldRenderer;
import com.rae.crowns.content.sound.CrownsSoundScapes;
import com.rae.crowns.content.thermodynamics.turbine.SteamFlowManager;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.rae.crowns.init.client.ShaderInit.volumeShader;
import static net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
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

    /** Render every frame */
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {

        if (volumeShader == null || event.getStage() != AFTER_PARTICLES) return;

        PoseStack poseStack = event.getPoseStack();
        SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();

        Vec3 cameraPos = event.getCamera().getPosition();
        int maxSteps = 256;
        float stepScale = 5f;

        VolumeWorldRenderer.render(poseStack, buffers, volumeShader, cameraPos, maxSteps);

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

    }

    protected static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }

}
