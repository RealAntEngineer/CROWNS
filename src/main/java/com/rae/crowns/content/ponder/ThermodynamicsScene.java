package com.rae.crowns.content.ponder;

import com.rae.crowns.api.flow.client.FlowParticleData;
import com.rae.crowns.api.flow.commun.FlowLine;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ThermodynamicsScene {
    public static void turbine(SceneBuilder sceneBuilder, SceneBuildingUtil sceneBuildingUtil) {
        sceneBuilder.title("turbine", "Turbines");
        sceneBuilder.configureBasePlate(0, -2,7);
        sceneBuilder.rotateCameraY(10);
        sceneBuilder.world.showSection(sceneBuildingUtil.select.everywhere(), Direction.DOWN);

        Selection pipeInput = sceneBuildingUtil.select.fromTo(4,1,1,7,1,1);
        sceneBuilder.overlay.showSelectionWithText(pipeInput, 30).text("input vapor into the steam inputs");
        sceneBuilder.idleSeconds(2);

        Selection turbines = sceneBuildingUtil.select.fromTo(0,1,1,2,1,1);
        sceneBuilder.overlay.showSelectionWithText(turbines, 30).text("if the vapor has sufficient quality (x > 0%) it will make the turbine turns");

        for (int i = 0; i <10; i++) {
            FlowLine spline1 =
                    new FlowLine(List.of(new Vec3(2.5,2.5,1.5),new Vec3(0,2.5,1.5))
                            ,
                            List.of(0.1d),
                            List.of(new Color(0f,0f,1f,1f))
                    );
            FlowLine spline2 =
                    new FlowLine(List.of(new Vec3(2.5,0.5,1.5),new Vec3(0,0.5,1.5))
                            ,
                            List.of(0.1d),
                            List.of(new Color(0f,0f,1f,1f))
                    );
            FlowLine spline3 =
                    new FlowLine(List.of(new Vec3(2.5,1.5,0.5),new Vec3(0,1.5,0.5))
                            ,
                            List.of(0.1d),
                            List.of(new Color(0f,0f,1f,1f))
                    );
            FlowLine spline4 =
                    new FlowLine(List.of(new Vec3(2.5,1.5,2.5),new Vec3(0,2.5,1.5))
                            ,
                            List.of(0.1d),
                            List.of(new Color(0f,0f,1f,1f))
                    );
            sceneBuilder.addInstruction(scene -> {
                PonderWorld world = scene.getWorld();
                world.addParticle(new FlowParticleData(spline1,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline2,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline3,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline4,0.1),  2,2,1, -1, 0, 0);
            });
            sceneBuilder.idle(1);
        }
        sceneBuilder.idle(10);
        sceneBuilder.world.setKineticSpeed(turbines, 256);
        for (int i = 0; i <30; i++) {
            FlowLine spline1 =
                    new FlowLine(List.of(new Vec3(2.5,2.5,1.5),new Vec3(0,2.5,1.5))
                            ,
                            List.of(0.1d),
                            List.of(Color.WHITE)
                    );
            FlowLine spline2 =
                    new FlowLine(List.of(new Vec3(2.5,0.5,1.5),new Vec3(0,0.5,1.5))
                            ,
                            List.of(0.1d),
                            List.of(Color.WHITE)
                    );
            FlowLine spline3 =
                    new FlowLine(List.of(new Vec3(2.5,1.5,0.5),new Vec3(0,1.5,0.5))
                            ,
                            List.of(0.1d),
                            List.of(Color.WHITE)
                    );
            FlowLine spline4 =
                    new FlowLine(List.of(new Vec3(2.5,1.5,2.5),new Vec3(0,2.5,1.5))
                            ,
                            List.of(0.1d),
                            List.of(Color.WHITE)
                    );
            sceneBuilder.addInstruction(scene -> {
                PonderWorld world = scene.getWorld();
                world.addParticle(new FlowParticleData(spline1,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline2,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline3,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline4,0.1),  2,2,1, -1, 0, 0);
            });
            sceneBuilder.idle(1);
        }


        sceneBuilder.markAsFinished();
    }
}
