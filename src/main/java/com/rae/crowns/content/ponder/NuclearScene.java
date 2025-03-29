package com.rae.crowns.content.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;

public class NuclearScene {
    public static void reactor(SceneBuilder builder, SceneBuildingUtil sceneBuildingUtil) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_reactor", "Nuclear Rectors");
        //sceneBuilder.setSceneOffsetY(-5);
        scene.scaleSceneView(0.4f);


        Selection layer0 = sceneBuildingUtil.select().layer(0);
        scene.world().showSection(layer0, Direction.DOWN);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().fromTo(4, 0, 4, 9, 0, 4),20).text("pump water from one side");
        scene.idleSeconds(2);
        scene.addKeyframe();

        Selection he0 = sceneBuildingUtil.select().fromTo(6,1,5,6,5,5);
        scene.world().showSection(he0, Direction.UP);
        Selection he1 = sceneBuildingUtil.select().fromTo(5,1,6,5,5,6);
        scene.world().showSection(he1, Direction.UP);
        Selection he2 = sceneBuildingUtil.select().fromTo(5,1,4,5,5,4);
        scene.world().showSection(he2, Direction.UP);
        Selection he3 = sceneBuildingUtil.select().fromTo(4,1,5,4,5,5);
        scene.world().showSection(he3, Direction.UP);
        scene.overlay().showOutlineWithText(he0,40).text("make column of heat exchanger to give time for the water to boil");

        int interval1 = 20;
        Selection layer1 = sceneBuildingUtil.select().layer(1);
        scene.world().showSection(layer1, Direction.DOWN);

        scene.idle(interval1);
        Selection layer2 = sceneBuildingUtil.select().layer(2);
        scene.world().showSection(layer2, Direction.DOWN);
        scene.idle(interval1);
        Selection layer3 = sceneBuildingUtil.select().layer(3);
        scene.world().showSection(layer3, Direction.DOWN);
        scene.idle(interval1);
        Selection layer4 = sceneBuildingUtil.select().layer(4);
        scene.world().showSection(layer4, Direction.DOWN);
        scene.idle(interval1);
        Selection layer5 = sceneBuildingUtil.select().layer(5);
        scene.world().showSection(layer5, Direction.DOWN);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().layers(1,5),60).text("build a chest board of fuel assembly and water or coal blocks to make the nuclear reaction occurs");
        scene.idleSeconds(2);
        scene.addKeyframe();

        Selection layer6 = sceneBuildingUtil.select().layer(6);
        scene.world().showSection(layer6, Direction.DOWN);
        scene.idle(10);
        Selection layer7 = sceneBuildingUtil.select().layer(7);
        scene.world().showSection(layer7, Direction.DOWN);
        scene.idle(10);
        Selection layer8 = sceneBuildingUtil.select().layer(8);
        scene.world().showSection(layer8, Direction.DOWN);
        scene.idle(10);
        Selection layer9 = sceneBuildingUtil.select().layer(9);
        scene.world().showSection(layer9, Direction.DOWN);
        scene.idle(10);

        Selection lastTank = sceneBuildingUtil.select().position(4,9,4);
        scene.overlay().showOutlineWithText(lastTank,60).text("fluid tank are used to merge the flow of vapor");

        scene.markAsFinished();
    }
}
