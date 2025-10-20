package com.rae.flow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!isGameActive())
            return;

        Level world = Minecraft.getInstance().level;
        FlowManager.getINSTANCE().tickFlow();
    }

    /*@SubscribeEvent
    public static void onRenderWorld(RenderLevelLastEvent event) {
        PoseStack ms = event.getPoseStack();
        ms.pushPose();
        SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
        float partialTicks = AnimationTickHolder.getPartialTicks();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera()
                .getPosition();
        FlowManager.getINSTANCE().renderFlow(ms, buffer, camera, partialTicks);
    }*/
    protected static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }
}
