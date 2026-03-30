package com.rae.crowns.init.misc;

import com.rae.crowns.content.hazards.HazardData;
import com.rae.crowns.content.hazards.HazardSystem;
import com.rae.crowns.content.hazards.radiation.ItemRadiation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@SuppressWarnings("unused") // Shut the fuck up
public class HazardInit {

    // Other stuff
    public static final ItemRadiation.DecayContainer raw_uranium = new ItemRadiation.DecayContainer(91_000_000F, 1F, 4.27D, 49.5 / 1000D, 0.0008);
    public static final ItemRadiation.DecayContainer solid_corium = new ItemRadiation.DecayContainer(822_600_000_000F, 0.25F, 5.5D, 0.75F, 1F, 0F, 0F, 661D, 0.946); // Ridiculous
    // Ingots
    public static final ItemRadiation.DecayContainer nu = new ItemRadiation.DecayContainer(127_000_000F, 1F, 4.27D, 49.5 / 1000D, 0.0008);
    public static final ItemRadiation.DecayContainer u235 = new ItemRadiation.DecayContainer(400_055_000F, 1F, 4.68D, 49.37 / 1000D, 0.25);
    public static final ItemRadiation.DecayContainer u238 = new ItemRadiation.DecayContainer(62_225_000F, 1F, 4.27D, 49.5 / 1000D, 0.0008);
    // Modifiers
    private static final double nugget = 1 / 9F; // a nugget is one 9th of an ingot
    private static final double block = 1 * 9F; // a block is 9 ingots

    public static void register(final FMLCommonSetupEvent event) {
        event.enqueueWork(HazardInit::registerItems);
    }

    public static void registerItems() {
        // Ore
        HazardSystem.register(ItemInit.RAW_URANIUM.get(), makeNuclearData(raw_uranium));

        // Ingots
        HazardSystem.register(ItemInit.DEPLETED_URANIUM_INGOT.get(), makeNuclearData(u238));
        HazardSystem.register(ItemInit.URANIUM_INGOT.get(), makeNuclearData(nu));
        HazardSystem.register(ItemInit.ENRICHED_URANIUM_INGOT.get(), makeNuclearData(u235));

        // Nuggets
        HazardSystem.register(ItemInit.DEPLETED_URANIUM_NUGGET.get(), makeNuclearData(u238.multiply(nugget)));
        HazardSystem.register(ItemInit.NATURAL_URANIUM_NUGGET.get(), makeNuclearData(nu.multiply(nugget)));
        HazardSystem.register(ItemInit.ENRICHED_URANIUM_NUGGET.get(), makeNuclearData(u235.multiply(nugget)));

        // Blocks
        HazardSystem.register(BlockInit.DEPLETED_URANIUM_BLOCK.get(), makeNuclearData(u238.multiply(block)));
        HazardSystem.register(BlockInit.SOLID_CORIUM.get(), makeNuclearData(solid_corium));
    }

    // Looks a bit nice even if redundant
    private static HazardData makeNuclearData(ItemRadiation.DecayContainer container) {
        return new HazardData().addEntry(container);
    }
}