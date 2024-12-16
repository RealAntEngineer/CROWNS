package com.rae.crowns.content.ponder;

import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
import net.minecraft.core.Direction;

public class NuclearScene {
    public static void reactor(SceneBuilder sceneBuilder, SceneBuildingUtil sceneBuildingUtil) {
        sceneBuilder.title("nuclear_reactor", "Nuclear Rectors");
        //sceneBuilder.setSceneOffsetY(-5);
        sceneBuilder.scaleSceneView(0.4f);


        Selection layer0 = sceneBuildingUtil.select.layer(0);
        sceneBuilder.world.showSection(layer0, Direction.DOWN);
        sceneBuilder.overlay.showSelectionWithText(sceneBuildingUtil.select.fromTo(4, 0, 4, 9, 0, 4),20).text("pump water from one side");
        sceneBuilder.idleSeconds(2);
        sceneBuilder.addKeyframe();

        Selection he0 = sceneBuildingUtil.select.fromTo(6,1,5,6,5,5);
        sceneBuilder.world.showSection(he0, Direction.UP);
        Selection he1 = sceneBuildingUtil.select.fromTo(5,1,6,5,5,6);
        sceneBuilder.world.showSection(he1, Direction.UP);
        Selection he2 = sceneBuildingUtil.select.fromTo(5,1,4,5,5,4);
        sceneBuilder.world.showSection(he2, Direction.UP);
        Selection he3 = sceneBuildingUtil.select.fromTo(4,1,5,4,5,5);
        sceneBuilder.world.showSection(he3, Direction.UP);
        sceneBuilder.overlay.showSelectionWithText(he0,40).text("make column of heat exchanger to give time for the water to boil");

        int interval1 = 20;
        Selection layer1 = sceneBuildingUtil.select.layer(1);
        sceneBuilder.world.showSection(layer1, Direction.DOWN);

        sceneBuilder.idle(interval1);
        Selection layer2 = sceneBuildingUtil.select.layer(2);
        sceneBuilder.world.showSection(layer2, Direction.DOWN);
        sceneBuilder.idle(interval1);
        Selection layer3 = sceneBuildingUtil.select.layer(3);
        sceneBuilder.world.showSection(layer3, Direction.DOWN);
        sceneBuilder.idle(interval1);
        Selection layer4 = sceneBuildingUtil.select.layer(4);
        sceneBuilder.world.showSection(layer4, Direction.DOWN);
        sceneBuilder.idle(interval1);
        Selection layer5 = sceneBuildingUtil.select.layer(5);
        sceneBuilder.world.showSection(layer5, Direction.DOWN);
        sceneBuilder.overlay.showSelectionWithText(sceneBuildingUtil.select.layers(1,5),60).text("build a chest board of fuel assembly and water or coal blocks to make the nuclear reaction occurs");
        sceneBuilder.idleSeconds(2);
        sceneBuilder.addKeyframe();

        Selection layer6 = sceneBuildingUtil.select.layer(6);
        sceneBuilder.world.showSection(layer6, Direction.DOWN);
        sceneBuilder.idle(10);
        Selection layer7 = sceneBuildingUtil.select.layer(7);
        sceneBuilder.world.showSection(layer7, Direction.DOWN);
        sceneBuilder.idle(10);
        Selection layer8 = sceneBuildingUtil.select.layer(8);
        sceneBuilder.world.showSection(layer8, Direction.DOWN);
        sceneBuilder.idle(10);
        Selection layer9 = sceneBuildingUtil.select.layer(9);
        sceneBuilder.world.showSection(layer9, Direction.DOWN);
        sceneBuilder.idle(10);

        Selection lastTank = sceneBuildingUtil.select.position(4,9,4);
        sceneBuilder.overlay.showSelectionWithText(lastTank,60).text("fluid tank are used to merge the flow of vapor");

        sceneBuilder.markAsFinished();
    }
}
