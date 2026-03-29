package com.rae.crowns.init.misc;

import com.rae.crowns.content.hazards.HazardData;
import com.rae.crowns.content.hazards.HazardSystem;
import com.rae.crowns.content.hazards.ItemRadiation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@SuppressWarnings("unused") // Shut the fuck up
public class HazardInit {

    // Modifiers
    private static final double nugget = 1/9F; // a nugget is one 9th of an ingot
    private static final double block = 1*9F; // a block is 9 ingots

    // Other stuff
    public static final ItemRadiation.DecayContainer raw_uranium = new ItemRadiation.DecayContainer(91_000_000F, 1F, 4.27D, 49.5/1000D, 0.0008);
    public static final ItemRadiation.DecayContainer solid_corium = new ItemRadiation.DecayContainer(822_600_000_000F, 0.25F, 5.5D, 0.75F, 1F, 0F, 0F, 661D, 0.946); // Ridiculous

    // Ingots
    public static final ItemRadiation.DecayContainer nu = new ItemRadiation.DecayContainer(127_000_000F, 1F, 4.27D, 49.5/1000D, 0.0008);
    public static final ItemRadiation.DecayContainer u235 = new ItemRadiation.DecayContainer(400_055_000F, 1F,4.68D, 49.37/1000D, 0.25);
    public static final ItemRadiation.DecayContainer u238 = new ItemRadiation.DecayContainer(62_225_000F, 1F, 4.27D, 49.5/1000D, 0.0008);

    // Fuel rods
    public static final ItemRadiation.DecayContainer fuel_rod_nu = nu.multiply(3);
    public static final ItemRadiation.DecayContainer fuel_rod_leu = u235.multiply(2).add(u238.specific_activity);
    public static final ItemRadiation.DecayContainer fuel_rod_heu = u235.multiply(3);

    // Looks a bit nice even if redundant
    private static HazardData makeData(ItemRadiation.DecayContainer container) { return new HazardData().addEntry(container); }

    public static void registerItems() {
        // Ore
        HazardSystem.register(ItemInit.RAW_URANIUM.get(), makeData(raw_uranium));

        // Ingots
        HazardSystem.register(ItemInit.DEPLETED_URANIUM_INGOT.get(), makeData(u238));
        HazardSystem.register(ItemInit.URANIUM_INGOT.get(), makeData(nu));
        HazardSystem.register(ItemInit.ENRICHED_URANIUM_INGOT.get(), makeData(u235));

        // Nuggets
        HazardSystem.register(ItemInit.DEPLETED_URANIUM_NUGGET.get(), makeData(u238.multiply(nugget)));
        HazardSystem.register(ItemInit.NATURAL_URANIUM_NUGGET.get(), makeData(nu.multiply(nugget)));
        HazardSystem.register(ItemInit.ENRICHED_URANIUM_NUGGET.get(), makeData(u235.multiply(nugget)));

        // Blocks
        HazardSystem.register(BlockInit.DEPLETED_URANIUM_BLOCK.get(), makeData(u238.multiply(block)));
        HazardSystem.register(BlockInit.SOLID_CORIUM.get(), makeData(solid_corium));
    }

    public static void register(final FMLCommonSetupEvent event) {
        event.enqueueWork(HazardInit::registerItems);
    }
}