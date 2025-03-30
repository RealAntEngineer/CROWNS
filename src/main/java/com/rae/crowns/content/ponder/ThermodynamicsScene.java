package com.rae.crowns.content.ponder;

import com.rae.crowns.api.flow.client.FlowParticleData;
import com.rae.crowns.api.flow.commun.FlowLine;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ThermodynamicsScene {
    public static void turbine(SceneBuilder builder, SceneBuildingUtil sceneBuildingUtil) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("turbine", "Turbines");
        scene.configureBasePlate(0, -2,7);
        scene.rotateCameraY(10);
        scene.world().showSection(sceneBuildingUtil.select().everywhere(), Direction.DOWN);

        Selection pipeInput = sceneBuildingUtil.select().fromTo(4,1,1,7,1,1);
        scene.overlay().showOutlineWithText(pipeInput, 30).text("input vapor into the steam inputs");
        scene.idleSeconds(2);

        Selection turbines = sceneBuildingUtil.select().fromTo(0,1,1,2,1,1);
        scene.overlay().showOutlineWithText(turbines, 30).text("if the vapor has sufficient quality (x > 0%) it will make the turbine turns");

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
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();
                world.addParticle(new FlowParticleData(spline1,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline2,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline3,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline4,0.1),  2,2,1, -1, 0, 0);
            });
            scene.idle(1);
        }
        scene.idle(10);
        scene.world().setKineticSpeed(turbines, 256);
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
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();
                world.addParticle(new FlowParticleData(spline1,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline2,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline3,0.1),  2,2,1, -1, 0, 0);
                world.addParticle(new FlowParticleData(spline4,0.1),  2,2,1, -1, 0, 0);
            });
            scene.idle(1);
        }


        scene.markAsFinished();
    }
}
