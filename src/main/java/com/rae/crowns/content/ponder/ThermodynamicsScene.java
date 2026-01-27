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
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ThermodynamicsScene {
    public static void turbine(@NotNull SceneBuilder builder, @NotNull SceneBuildingUtil sceneBuildingUtil) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("turbine", "Turbines");
        scene.configureBasePlate(4, 4, 7);
        scene.rotateCameraY(10);
        scene.world().showSection(sceneBuildingUtil.select().everywhere(), Direction.DOWN);
        scene.world().modifyBlocks(sceneBuildingUtil.select().everywhere(), s -> {
            if (s.hasProperty(TurbineStageBlock.CASING))
                s = s.setValue(TurbineStageBlock.CASING,false);
            return s;
        }, false);

        Selection pipeInput = sceneBuildingUtil.select().fromTo(4, 1, 1, 7, 1, 1);
        scene.overlay().showOutlineWithText(pipeInput, 30).text("input vapor into the steam inputs");
        scene.idleSeconds(2);

        Selection turbines = sceneBuildingUtil.select().fromTo(0, 1, 1, 2, 1, 1);
        scene.overlay().showOutlineWithText(turbines, 30).text("if the vapor has sufficient quality (x > 0%) it will make the turbine turns");

        double startX = 10.5;
        for (int i = 0; i < 40; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(2, 2, 1);

                spawnFlow(world,
                        new Vec3(startX, 2.5, 1.5),
                        new Vec3(2,   2.5, 1.5),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, 0.5, 1.5),
                        new Vec3(2,   0.5, 1.5),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, 1.5, 0.5),
                        new Vec3(2,   1.5, 0.5),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, 1.5, 2.5),
                        new Vec3(2,   1.5, 2.5),
                        spawn,List.of(new Color(0f, 0f, 1f, 1f))
                );
            });
            scene.idle(1);
        }
        scene.idle(10);
        scene.world().setKineticSpeed(turbines, 256);
        for (int i = 0; i < 40; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(2, 2, 1);

                spawnFlow(world,
                        new Vec3(startX, 2.5, 1.5),
                        new Vec3(0,   2.5, 1.5),
                        spawn,List.of(Color.WHITE)
                );

                spawnFlow(world,
                        new Vec3(startX, 0.5, 1.5),
                        new Vec3(0,   0.5, 1.5),
                        spawn,List.of(Color.WHITE)
                );

                spawnFlow(world,
                        new Vec3(startX, 1.5, 0.5),
                        new Vec3(0,   1.5, 0.5),
                        spawn,List.of(Color.WHITE)
                );

                spawnFlow(world,
                        new Vec3(startX, 1.5, 2.5),
                        new Vec3(0,   1.5, 2.5),
                        spawn,List.of(Color.WHITE)
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
