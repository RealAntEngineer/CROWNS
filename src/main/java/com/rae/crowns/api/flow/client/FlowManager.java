package com.rae.crowns.api.flow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rae.crowns.api.flow.commun.FlowLine;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlowManager {
    private final Map<Object, FlowEntry> flows = Collections.synchronizedMap(new HashMap<>());
    private static FlowManager INSTANCE;

    public static FlowManager getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new FlowManager();

        }
        return INSTANCE;
    }
    public void tickFlow(){

    }
    public void renderFlow(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        flows.forEach((key, entry) -> {
            FlowLine flow = entry.getFlow();
            List<Color> colors = flow.getColorsAtPoints();
            AtomicBoolean cancelRender = new AtomicBoolean(false);
            colors.forEach(c-> {
                        c.setAlpha(1f);
                        if (entry.isFading()) {
                            int prevTicks = entry.ticksTillRemoval + 1;
                            float fadeticks = Outliner.OutlineEntry.FADE_TICKS;
                            float lastAlpha = prevTicks >= 0 ? 1 : 1 + (prevTicks / fadeticks);
                            float currentAlpha = 1 + (entry.ticksTillRemoval / fadeticks);
                            float alpha = Mth.lerp(pt, lastAlpha, currentAlpha);

                            c.setAlpha( alpha * alpha * alpha);

                            if (c.getAlpha() < 1 / 8f)
                                cancelRender.set(true);
                        }
                    });
            if (cancelRender.get()){
                return;
            }
            flow.render(ms, buffer, camera, pt);
        });
    }

    public static class FlowEntry {
        public static final int FADE_TICKS = 8;

        private final FlowLine flow;
        private int ticksTillRemoval = 1;

        public FlowEntry(FlowLine flow) {
            this.flow = flow;
        }

        public FlowLine getFlow() {
            return flow;
        }

        public int getTicksTillRemoval() {
            return ticksTillRemoval;
        }

        public boolean isAlive() {
            return ticksTillRemoval >= -FADE_TICKS;
        }

        public boolean isFading() {
            return ticksTillRemoval < 0;
        }

        public void tick() {
            ticksTillRemoval--;
            //flow.tick();
        }
    }

}
