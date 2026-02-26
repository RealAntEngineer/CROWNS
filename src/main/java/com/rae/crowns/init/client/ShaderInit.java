package com.rae.crowns.init.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.rae.crowns.CROWNS;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = "crowns", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShaderInit {

    public static ShaderInstance volumeShader;
    static RenderStateShard.@NotNull ShaderStateShard VOLUME_FULL = new RenderStateShard.ShaderStateShard.ShaderStateShard(() -> ShaderInit.volumeShader);

    @SubscribeEvent
    public static void registerShaders(@NotNull RegisterShadersEvent event) {
        try {
            // Register your shader
            ResourceProvider resourceProvider = event.getResourceProvider();
            event.registerShader(new ShaderInstance(resourceProvider, CROWNS.resource("volume_shader"),
                    DefaultVertexFormat.POSITION), shader -> volumeShader = shader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
