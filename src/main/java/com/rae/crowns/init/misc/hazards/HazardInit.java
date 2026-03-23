package com.rae.crowns.init.misc.hazards;

import com.rae.crowns.init.misc.ItemInit;
import com.rae.crowns.init.misc.hazards.types.HazardTypeBase;
import com.rae.crowns.init.misc.hazards.types.HazardTypeRadiation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@SuppressWarnings("unused") // Oh shut up
public class HazardInit {

    // Modifiers
    private static final double nugget = 1/9F; // a nugget is one 9th of an ingot
    private static final double block = 1*9F; // a block is 9 ingots


    // Forget the old rad/s system, will now use Bq. Assume all ingots are 5kg

    // Other stuff
    public static final double raw_uranium = 152_000F;

    // Ingots
    public static final double nu = 127_000_000F;
    public static final double u235 = 400_055_000F;
    public static final double u238 = 62_225_000F;

    // Fuel assemblies
    public static final double nu_assembly = (nu * 3);
    public static final double leu_assembly = (u238 * 2) + u235;
    public static final double heu_assembly = (u235 * 3);

    // Types
    public static final HazardTypeBase RADIATION = new HazardTypeRadiation();

    // Makes data, duh
    private static HazardData makeData() { return new HazardData(); }
    private static HazardData makeData(HazardTypeBase hazardType) { return new HazardData().addEntry(hazardType); }
    private static HazardData makeData(HazardTypeBase hazardType, double level) { return new HazardData().addEntry(hazardType, level); }

    public static void registerItems() {
        // Ingots
        HazardSystem.register(ItemInit.URANIUM_INGOT.get(), makeData(RADIATION, nu));
        HazardSystem.register(ItemInit.DEPLETED_URANIUM_INGOT.get(), makeData(RADIATION, u238));
        HazardSystem.register(ItemInit.ENRICHED_URANIUM_INGOT.get(), makeData(RADIATION, u235));

        // Nuggets
        HazardSystem.register(ItemInit.NATURAL_URANIUM_NUGGET.get(), makeData(RADIATION, nu * nugget));
        HazardSystem.register(ItemInit.DEPLETED_URANIUM_INGOT.get(), makeData(RADIATION, u238 * nugget));
        HazardSystem.register(ItemInit.ENRICHED_URANIUM_NUGGET.get(), makeData(RADIATION, u235 * nugget));

        // Fuel rod bullshit and blockitems i'll do later
    }

    public static void register(final FMLCommonSetupEvent event) {
        event.enqueueWork(HazardInit::registerItems);
    }
}