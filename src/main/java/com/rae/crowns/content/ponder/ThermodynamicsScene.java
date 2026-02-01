package com.rae.crowns.content.ponder;

import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.rae.flow.client.FlowParticleData;
import com.rae.flow.commun.FlowLine;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ThermodynamicsScene {
    public static void turbine(@NotNull SceneBuilder builder, @NotNull SceneBuildingUtil sceneBuildingUtil) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("turbine", "Turbines");
        scene.configureBasePlate(1, -3, 12);
        scene.rotateCameraY(10);
        scene.world().showSection(sceneBuildingUtil.select().everywhere(), Direction.DOWN);
        scene.world().modifyBlocks(sceneBuildingUtil.select().everywhere(), s -> {
            if (s.hasProperty(TurbineStageBlock.CASING))
                s = s.setValue(TurbineStageBlock.CASING,false);
            return s;
        }, false);

        Selection pipeInput = sceneBuildingUtil.select().position(10, 2, 4)
                .add(sceneBuildingUtil.select().position(10, 2, 2))
                .add(sceneBuildingUtil.select().position(10, 3, 3))
                .add(sceneBuildingUtil.select().position(10, 1, 3));
        scene.overlay().showOutlineWithText(pipeInput, 30).text("input vapor into the steam inputs");
        scene.idleSeconds(2);

        double startX = 10.5;
        double endX = 2;
        double turbineY = 2.5;
        double turbineZ = 3.5;
        Selection turbines = sceneBuildingUtil.select().fromTo((int) (startX-0.5), 2, 3, 2, 2, 3);
        scene.overlay().showOutlineWithText(turbines, 30)
                .text("if the water is not hot enough the turbine will not turn");


        for (int i = 0; i < 40; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(startX, turbineY, turbineZ);

                spawnFlow(world,
                        new Vec3(startX, turbineY + 1, turbineZ),
                        new Vec3(endX,   turbineY + 1, turbineZ),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY - 1, turbineZ),
                        new Vec3(endX,   turbineY - 1, turbineZ),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ - 1),
                        new Vec3(endX,   turbineY, turbineZ - 1),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ + 1),
                        new Vec3(endX,   turbineY, turbineZ + 1),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );
            });
            scene.idle(1);
        }
        scene.idle(10);
        scene.world().setKineticSpeed(turbines, 256);
        scene.overlay().showOutlineWithText(turbines, 60)
                .text("once the water is boiling and has some vapor in it the turbine will spin");
        for (int i = 0; i < 60; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(2, 2, 1);

                spawnFlow(world,
                        new Vec3(startX, turbineY + 1, turbineZ),
                        new Vec3(endX,   turbineY + 1, turbineZ),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY - 1, turbineZ),
                        new Vec3(endX,   turbineY - 1, turbineZ),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ - 1),
                        new Vec3(endX,   turbineY, turbineZ - 1),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ + 1),
                        new Vec3(endX,   turbineY, turbineZ + 1),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );
            });
            scene.idle(1);
        }
        scene.idle(10);
        scene.overlay().showText( 90).text(
                "the color show the vapor quality"
        );

        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(9,2,2), 90)
                .text("from pure steam (x = 100%)");
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(3,2,2), 90)
                .text("to pure liquid water (x = 0%)");
        for (int i = 0; i < 120; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(2, 2, 1);

                spawnFlow(world,
                        new Vec3(startX, turbineY + 1, turbineZ),
                        new Vec3(endX,   turbineY + 1, turbineZ),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY - 1, turbineZ),
                        new Vec3(endX,   turbineY - 1, turbineZ),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ - 1),
                        new Vec3(endX,   turbineY, turbineZ - 1),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ + 1),
                        new Vec3(endX,   turbineY, turbineZ + 1),
                        spawn,List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );
            });
            scene.idle(1);
        }


        scene.markAsFinished();
    }

    private static void spawnFlow(
            PonderLevel world,
            Vec3 from,
            Vec3 to,
            Vec3 spawnPos,
            List<Color> colors
    ) {
        FlowLine spline = new FlowLine(
                List.of(from, to),
                List.of(0.1d),
                colors
        );

        world.addParticle(
                new FlowParticleData(spline, 0.1),
                spawnPos.x, spawnPos.y, spawnPos.z,
                -1, 0, 0
        );
    }

}
