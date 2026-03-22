package com.rae.crowns.content.ponder;

import com.rae.crowns.content.thermodynamics.compressor.CompressorBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.rae.flow.client.FlowParticleData;
import com.rae.flow.commun.FlowLine;
import com.rae.formicapi.FormicApiLang;
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
                s = s.setValue(TurbineStageBlock.CASING, false);
            return s;
        }, false);

        Selection pipeInput = sceneBuildingUtil.select().position(10, 2, 4)
                .add(sceneBuildingUtil.select().position(10, 2, 2))
                .add(sceneBuildingUtil.select().position(10, 3, 3))
                .add(sceneBuildingUtil.select().position(10, 1, 3));
        scene.overlay().showOutlineWithText(pipeInput, 20 * 4).text("Input vapor into the steam inputs");
        scene.idleSeconds(4);

        double startX = 10.5;
        double endX = 2;
        double turbineY = 2.5;
        double turbineZ = 3.5;
        Selection turbines = sceneBuildingUtil.select().fromTo((int) (startX - 0.5), 2, 3, 2, 2, 3);
        scene.overlay().showOutlineWithText(turbines, 20 * 5)
                .text("If the water is not hot enough the turbine will not turn");


        for (int i = 0; i < 20 * 5; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(startX, turbineY, turbineZ);

                spawnFlow(world,
                        new Vec3(startX, turbineY + 1, turbineZ),
                        new Vec3(endX, turbineY + 1, turbineZ),
                        spawn, List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY - 1, turbineZ),
                        new Vec3(endX, turbineY - 1, turbineZ),
                        spawn, List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ - 1),
                        new Vec3(endX, turbineY, turbineZ - 1),
                        spawn, List.of(new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ + 1),
                        new Vec3(endX, turbineY, turbineZ + 1),
                        spawn, List.of(new Color(0f, 0f, 1f, 1f))
                );
            });
            scene.idle(1);
        }
        scene.idle(10);
        scene.world().setKineticSpeed(turbines, 256);
        scene.overlay().showOutlineWithText(turbines, 20 * 6)
                .text("Once the water is boiling and has some vapor in it the turbine will spin");
        for (int i = 0; i < 20 * 7; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(2, 2, 1);

                spawnFlow(world,
                        new Vec3(startX, turbineY + 1, turbineZ),
                        new Vec3(endX, turbineY + 1, turbineZ),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY - 1, turbineZ),
                        new Vec3(endX, turbineY - 1, turbineZ),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ - 1),
                        new Vec3(endX, turbineY, turbineZ - 1),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ + 1),
                        new Vec3(endX, turbineY, turbineZ + 1),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );
            });
            scene.idle(1);
        }
        scene.idle(10);
        scene.overlay().showText(20 * 6).text(
                "The color shows the vapor quality"
        );

        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(9, 2, 2), 20 * 6)
                .text("From pure steam (x = 100%%)");
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(3, 2, 2), 20 * 6)
                .text("To pure liquid water (x = 0%%)");
        for (int i = 0; i < 20 * 8; i++) {
            scene.addInstruction(ponderScene -> {
                PonderLevel world = ponderScene.getWorld();

                Vec3 spawn = new Vec3(2, 2, 1);

                spawnFlow(world,
                        new Vec3(startX, turbineY + 1, turbineZ),
                        new Vec3(endX, turbineY + 1, turbineZ),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY - 1, turbineZ),
                        new Vec3(endX, turbineY - 1, turbineZ),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ - 1),
                        new Vec3(endX, turbineY, turbineZ - 1),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );

                spawnFlow(world,
                        new Vec3(startX, turbineY, turbineZ + 1),
                        new Vec3(endX, turbineY, turbineZ + 1),
                        spawn, List.of(Color.WHITE, new Color(0f, 0f, 1f, 1f))
                );
            });
            scene.idle(1);
        }


        scene.markAsFinished();
    }

    private static void spawnFlow(
            @NotNull PonderLevel world,
            @NotNull Vec3 from,
            @NotNull Vec3 to,
            @NotNull Vec3 spawnPos,
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

    public static void compressor(@NotNull SceneBuilder builder, @NotNull SceneBuildingUtil sceneBuildingUtil) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("compressor", "Compressor");
        scene.configureBasePlate(0, 0, 8);
        scene.rotateCameraY(10);
        scene.world().showSection(sceneBuildingUtil.select().everywhere(), Direction.DOWN);

        scene.overlay().showText(20 * 4)
                .text("Compressor increase the pressure of the incoming flow");
        scene.idleSeconds(8);
        scene.overlay().showText(20 * 4)
                .text("At the difference of the turbine the increase in pressure depends on the speed");

        scene.idleSeconds(8);
        scene.addKeyframe();
        scene.overlay().showText(20 * 15)
                .text("At 0 rpm it's %s\nAt 64 rpm it's %s\nAt 128 rpm it's %s\nAt 256 rpm it's %s",
                        "ΔP = "+FormicApiLang.formatPressure(0).string(),
                        "ΔP = " + FormicApiLang.formatPressure(CompressorBlockEntity.getPressureDelta(64)).string(),
                        "ΔP = " + FormicApiLang.formatPressure(CompressorBlockEntity.getPressureDelta(128)).string(),
                        "ΔP = " + FormicApiLang.formatPressure(CompressorBlockEntity.getPressureDelta(256)).string()
                );
        scene.markAsFinished();

    }

}
