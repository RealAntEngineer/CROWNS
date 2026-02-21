package com.rae.crowns.init.client;

import com.rae.crowns.content.ponder.NuclearScene;
import com.rae.crowns.content.ponder.ThermodynamicsScene;
import com.rae.crowns.init.misc.BlockInit;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PonderInit {

    public static void register(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Register storyboards here
        // (!) Added entries require re-launch
        // (!) Modifications inside storyboard methods only require re-opening the ui
        PonderSceneRegistrationHelper<ItemProviderEntry<?,?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);


        HELPER.forComponents(BlockInit.FUEL_ASSEMBLY, BlockInit.HEAT_EXCHANGER)
                .addStoryBoard("nuclear/reactor", NuclearScene::nuclearBasic)
                .addStoryBoard("nuclear/reactor_v3", NuclearScene::reactorLayout);
        HELPER.forComponents(BlockInit.STEAM_INPUT, BlockInit.TURBINE_STAGE, BlockInit.STEAM_COLLECTOR)
                .addStoryBoard("thermal/turbine_2", ThermodynamicsScene::turbine);
        HELPER.forComponents(BlockInit.COMPRESSOR)
                .addStoryBoard("thermal/compressor",ThermodynamicsScene::compressor);
    }
}