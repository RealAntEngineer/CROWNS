package com.rae.crowns.content.ponder;

import com.rae.crowns.content.nuclear.AssemblyBlock;
import com.rae.crowns.init.misc.BlockInit;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class NuclearScene {
    public static void nuclearBasic(@NotNull SceneBuilder builder, @NotNull SceneBuildingUtil sceneBuildingUtil) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("nuclear_basics", "Nuclear Rectors");
        //sceneBuilder.setSceneOffsetY(-5);
        scene.scaleSceneView(0.6f);
        scene.setSceneOffsetY(-2f);
        scene.world().setBlocks(sceneBuildingUtil.select().everywhere(), Blocks.AIR.defaultBlockState(), false);//clean slate
        BlockPos centerFuel = new BlockPos(5, 0, 3);
        BlockPos exteriorFuel = new BlockPos(2, 0, 3);
        scene.world().setBlock(centerFuel, BlockInit.FUEL_ASSEMBLY.getDefaultState(), false);
        scene.world().setBlock(exteriorFuel, BlockInit.FUEL_ASSEMBLY.getDefaultState(), false);

        scene.world().showSection(sceneBuildingUtil.select().position(centerFuel), Direction.UP);
        scene.world().showSection(sceneBuildingUtil.select().position(exteriorFuel), Direction.UP);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(centerFuel), 3 * 20)
                .text("nuclear fuel naturally produce fast neutrons");
        scene.idleSeconds(4);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(exteriorFuel), 3 * 20)
                .text("fast neutrons are unlikely to cause an other fuel block to undergo fission");
        scene.idleSeconds(4);

        BlockPos coal = new BlockPos(4, 0, 3);
        BlockPos water = new BlockPos(3, 0, 3);
        scene.world().setBlock(coal, Blocks.COAL_BLOCK.defaultBlockState(), false);
        scene.world().setBlock(water, Blocks.WATER.defaultBlockState(), false);

        scene.world().showSection(sceneBuildingUtil.select().position(coal), Direction.UP);
        scene.world().showSection(sceneBuildingUtil.select().position(water), Direction.UP);
        scene.overlay().showText(4 * 20).text("add moderator to transform them into thermal neutrons that can induce fission and produce more neutrons");
        scene.idleSeconds(5);
        scene.overlay().showText(4 * 20).text("in short a moderator make the reactor hotter");
        scene.idleSeconds(5);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(coal), 4 * 20).text("70% efficiency for coal");
        scene.idleSeconds(5);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().position(water), 4 * 20).text("50% efficiency for water");
        scene.idleSeconds(5);
        scene.world().setBlock(coal, Blocks.GOLD_BLOCK.defaultBlockState(), false);
        scene.overlay().showText(4 * 20).text("gold has the opposite effect and can be used as a control rod to slow down radiation");
        scene.idleSeconds(5);
        scene.addKeyframe();

        scene.world().setBlocks(sceneBuildingUtil.select().everywhere(), Blocks.AIR.defaultBlockState(), false);
        scene.idle(5);

        Selection mod = sceneBuildingUtil.select().fromTo(3, 0, 3, 3, 3, 3);
        Selection fc1 = sceneBuildingUtil.select().fromTo(3, 0, 4, 3, 3, 4);
        Selection fc2 = sceneBuildingUtil.select().fromTo(4, 0, 3, 4, 3, 3);
        Selection fc3 = sceneBuildingUtil.select().fromTo(2, 0, 3, 2, 3, 3);
        Selection fc4 = sceneBuildingUtil.select().fromTo(3, 0, 2, 3, 3, 2);
        Selection bb = sceneBuildingUtil.select().fromTo(2, 0, 2, 4, 3, 4);
        scene.world().setBlocks(mod, Blocks.COAL_BLOCK.defaultBlockState(), false);
        scene.world().showSection(mod, Direction.UP);
        scene.world().setBlocks(fc1, BlockInit.FUEL_ASSEMBLY.getDefaultState(), false);
        scene.world().showSection(fc1, Direction.UP);
        scene.world().setBlocks(fc2, BlockInit.FUEL_ASSEMBLY.getDefaultState(), false);
        scene.world().showSection(fc2, Direction.UP);
        scene.world().setBlocks(fc3, BlockInit.FUEL_ASSEMBLY.getDefaultState(), false);
        scene.world().showSection(fc3, Direction.UP);
        scene.world().setBlocks(fc4, BlockInit.FUEL_ASSEMBLY.getDefaultState(), false);
        scene.world().showSection(fc4, Direction.UP);
        scene.overlay().showOutlineWithText(bb, 10 * 20).text("when enough fuel are close to each other with a moderator");
        scene.idleSeconds(2);
        scene.world().modifyBlocks(fc1, blockState -> blockState.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);
        scene.world().modifyBlocks(fc2, blockState -> blockState.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);
        scene.world().modifyBlocks(fc3, blockState -> blockState.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);
        scene.world().modifyBlocks(fc4, blockState -> blockState.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);
        scene.idleSeconds(10);
        //scene.addKeyframe();
        scene.markAsFinished();
    }

    public static void reactorLayout(@NotNull SceneBuilder builder, @NotNull SceneBuildingUtil sceneBuildingUtil) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reactor_layout", "Reactor Layout");
        //sceneBuilder.setSceneOffsetY(-5);
        scene.scaleSceneView(0.6f);
        scene.setSceneOffsetY(-2f);

        scene.overlay().showText(60).text("like every hot blocks, you can use hot nuclear fuel to heat water");
        scene.idle(20);

        scene.world().restoreBlocks(sceneBuildingUtil.select().everywhere());

        Selection layer0 = sceneBuildingUtil.select().layers(0, 3);
        scene.world().showSection(layer0, Direction.DOWN);
        //scene.idleSeconds(2);


        Selection he = sceneBuildingUtil.select().fromTo(4, 1, 3, 4, 7, 3);
        scene.world().modifyBlocks(he, blockState -> blockState.setValue(ProperWaterloggedBlock.WATERLOGGED, false), false);
        scene.world().restoreBlocks(sceneBuildingUtil.select().everywhere());
        //scene.world().showSection(he, Direction.UP);

        Selection slice = sceneBuildingUtil.select().fromTo(0, 3, 3, 8, 8, 8);
        // 3,4, 5, 3, 8, 5
        scene.world().showSection(slice, Direction.UP);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().layers(1, 5), 100)
                .text("it's recommended to make reactors in a chest board manner");
        scene.idleSeconds(5);

        scene.overlay().showOutlineWithText(he, 40).text("the higher the reactor, the more time the water will have to boil");
        scene.idleSeconds(3);
        scene.overlay().showOutlineWithText(sceneBuildingUtil.select().fromTo(5, 3, 3, 5, 6, 3), 60)
                .text("fuel becomes red when it's hotter than 3000 degrees Kelvin, it explodes at 3500");
        scene.idleSeconds(3);
        scene.markAsFinished();
    }
}