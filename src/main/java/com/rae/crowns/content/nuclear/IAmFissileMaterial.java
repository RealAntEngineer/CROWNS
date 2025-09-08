package com.rae.crowns.content.nuclear;


import com.rae.crowns.CROWNS;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public interface IAmFissileMaterial {
    HashMap<ResourceLocation, Couple<Float>> fissileCrossSection = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"),Couple.create(1f,583f), //cross-section in barn
                    CROWNS.resource("u238"),Couple.create(0.3f,0.0001f),
                    CROWNS.resource("p239"),Couple.create(2f,748f)

            ));//for U235,U358 and Plutonium -> percentage of total mass
    HashMap<ResourceLocation,Float> molarConcentration = new HashMap<>(
                    Map.of(
                            CROWNS.resource("u235"),19/235f*10000, //amount of moles in a cubic meter of pure metal
                            CROWNS.resource("u238"),19/238f*10000,
                            CROWNS.resource("p239"),19/239f*10000

                    ));//for U235,U358 and Plutonium -> percentage of total mass

    // change that to a couple -> get the flux of neutron (fast,slow)
    // and return the amount transmitted (fast,slow)
    Couple<Float> absorbNeutrons(Couple<Float> radiationFlux);
    float getEffectiveK();
}
