package com.rae.crowns.init.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.rae.crowns.CROWNS;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class CrownsRenderTypes extends RenderStateShard {


    //maybe use something like that : https://github.com/bernie-g/geckolib/blob/main/common/src/main/java/software/bernie/geckolib/cache/texture/AutoGlowingTexture.java#L36

    private static final RenderType TCHERENKOV_RADIATION = RenderType.create(createLayerName("tcherenkov_radiation"),
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES,
            256, false, true, RenderType.CompositeState.builder()
                    .setShaderState(ShaderInit.VOLUME_FULL)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setCullState(CULL)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false));

    public static RenderType tcherenkovRadiation() {
        return TCHERENKOV_RADIATION;
    }

    private static String createLayerName(String name) {
        return CROWNS.MODID + ":" + name;
    }

    // Yummy protected fields
    private CrownsRenderTypes() {
        super(null, null, null);
    }
}
