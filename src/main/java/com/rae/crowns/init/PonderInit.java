package com.rae.crowns.init;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.ponder.NuclearScene;
import com.rae.crowns.content.ponder.ThermodynamicsScene;
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;

public class PonderInit {

	static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(CROWNS.MODID);

	public static void register() {
		// Register storyboards here
		// (!) Added entries require re-launch
		// (!) Modifications inside storyboard methods only require re-opening the ui
		HELPER.forComponents(BlockInit.FUEL_ASSEMBLY,BlockInit.HEAT_EXCHANGER).addStoryBoard("nuclear/reactor", NuclearScene::reactor);
		HELPER.forComponents(BlockInit.STEAM_INPUT,BlockInit.TURBINE_STAGE).addStoryBoard("thermal/turbine", ThermodynamicsScene::turbine);

	}
}
