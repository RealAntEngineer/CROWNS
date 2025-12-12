package com.rae.crowns.content.ponder;

import com.rae.crowns.content.nuclear.AssemblyBlock;
import com.rae.crowns.init.misc.BlockInit;
import com.rae.crowns.init.misc.FluidInit;
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
        scene.world().modifyBlocks(fc1, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.NONE), false);
        scene.world().modifyBlocks(fc2, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.NONE), false);
        scene.world().modifyBlocks(fc3, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.NONE), false);
        scene.world().modifyBlocks(fc4, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.NONE), false);

        scene.overlay()
                .showText(4 * 20)
                .text("As fission continues, fuel assemblies begin to heat up...");
        scene.idleSeconds(4);

        // 🔶 Medium heat
        scene.world().modifyBlocks(fc1, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);
        scene.world().modifyBlocks(fc2, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);
        scene.world().modifyBlocks(fc3, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);
        scene.world().modifyBlocks(fc4, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.LOW), false);

        scene.idleSeconds(3);

        // 🔴 Critical heat
        scene.overlay()
                .showText(4 * 20)
                .text("Without proper cooling or control rods, temperature reaches critical levels.");
        scene.world().modifyBlocks(fc1, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.HIGH), false);
        scene.world().modifyBlocks(fc2, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.HIGH), false);
        scene.world().modifyBlocks(fc3, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.HIGH), false);
        scene.world().modifyBlocks(fc4, s -> s.setValue(AssemblyBlock.ACTIVITY, AssemblyBlock.Activity.HIGH), false);

        scene.idleSeconds(3);

        scene.overlay()
                .showText(4 * 20)
                .text("The fuel catastrophically fails and melts into corium.");
        scene.idleSeconds(2);

        // 💥 Fuel collapses into corium
        scene.world().setBlocks(fc1, FluidInit.CORIUM.get().getFlowing().defaultFluidState().createLegacyBlock(), false);
        scene.world().setBlocks(fc2, FluidInit.CORIUM.get().getFlowing().defaultFluidState().createLegacyBlock(), false);
        scene.world().setBlocks(fc3, FluidInit.CORIUM.get().getFlowing().defaultFluidState().createLegacyBlock(), false);
        scene.world().setBlocks(fc4, FluidInit.CORIUM.get().getFlowing().defaultFluidState().createLegacyBlock(), false);

        scene.idleSeconds(3);
    }

    public static void reactorLayout(@NotNull SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reactor_layout", "Reactor Layout");
        //sceneBuilder.setSceneOffsetY(-5);
        scene.scaleSceneView(0.6f);
        scene.setSceneOffsetY(-2f);

        scene.overlay()
                .showText(60)
                .text("Like all hot blocks, nuclear fuel can be used to heat water.");
        scene.idle(20);

        // ✅ Reveal lower reactor casing
        Selection layer0 = util.select().layers(0, 3);
        scene.world().showSection(layer0, Direction.DOWN);
        scene.idleSeconds(2);

        // ✅ Outline the full reactor interior
        Selection slice = util.select().fromTo(0, 3, 3, 8, 8, 8);
        scene.world().showSection(slice, Direction.UP);

        scene.overlay()
                .showOutlineWithText(util.select().layers(1, 5), 100)
                .text("It's recommended to build reactors in a concentric circles.");
        scene.idleSeconds(5);

        // ✅ Highlight full reactor height (boiling efficiency)
        Selection reactorHeight = util.select().fromTo(4, 1, 3, 4, 7, 3);
        scene.overlay()
                .showOutlineWithText(reactorHeight, 40)
                .text("The taller the reactor, the more time water has to boil.");
        scene.idleSeconds(3);

        // ✅ Highlight overheated fuel cells
        scene.overlay()
                .showOutlineWithText(util.select().fromTo(5, 3, 3, 5, 6, 3), 60)
                .text("Fuel turns red above 3000K and explodes at 3500K.");
        scene.idleSeconds(3);

        scene.markAsFinished();

    }

    private static Selection getCircularReactorRing(SceneBuildingUtil util, int ringIndex) {
        // Reactor interior bounds
        int min = 1;
        int max = 7;

        int centerX = 4;
        int centerZ = 4;

        // 7x7 reactor radii
        int outerRadius = 3 - ringIndex;
        int innerRadius = outerRadius - 1;

        Selection result = null;

        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;

                double distSq = dx * dx + dz * dz;

                boolean insideOuter = distSq <= (outerRadius + 0.5) * (outerRadius + 0.5);
                boolean outsideInner = innerRadius < 0
                        || distSq > (innerRadius + 0.5) * (innerRadius + 0.5);

                if (insideOuter && outsideInner) {
                    Selection column = util.select().column(x, z);

                    if (result == null) {
                        result = column;        // ✅ seed
                    } else {
                        result = result.add(column);  // ✅ accumulate
                    }
                }
            }
        }

        return result;
    }


}