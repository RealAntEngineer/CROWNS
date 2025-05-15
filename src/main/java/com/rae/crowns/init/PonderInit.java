package com.rae.crowns.init;

import com.rae.crowns.content.ponder.NuclearScene;
import com.rae.crowns.content.ponder.ThermodynamicsScene;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class PonderInit {

	public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		// Register storyboards here
		// (!) Added entries require re-launch
		// (!) Modifications inside storyboard methods only require re-opening the ui
		PonderSceneRegistrationHelper<ItemProviderEntry<?,?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);


		HELPER.forComponents(BlockInit.FUEL_ASSEMBLY,BlockInit.HEAT_EXCHANGER).addStoryBoard("nuclear/reactor", NuclearScene::reactor);
		HELPER.forComponents(BlockInit.STEAM_INPUT, BlockInit.STEAM_COLLECTOR,BlockInit.TURBINE_STAGE).addStoryBoard("thermal/turbine", ThermodynamicsScene::turbine);

	}
}
