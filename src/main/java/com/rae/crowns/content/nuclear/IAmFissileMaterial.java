package com.rae.crowns.content.nuclear;


import com.rae.crowns.CROWNS;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public interface IAmFissileMaterial extends IAmRadioactiveSource {
    //calculate from cross-section (barn), depth (1 meter) and concentration ( as mox fuel isn't a 1m by 1m block of uranium)
    // Absorption law :
    // I = I0* exp(-ln ( dx * c * PI/4 +1 )/dx*L)
    // ( dx = 2*sqrt(sigma/pi) the diameter of a circle of cross-section sigma, c the concentration in mol.m-3 and L the length in m)

    //According to the wikipedia page : https://en.wikipedia.org/wiki/Neutron_cross_section
    // the correct formula is r = N * Flux * sigma
    // this should work only if r N is small ( here we are considering a huge volume of 1 cubic meter )
    // 800 moles of uranium for pure metal *  the mass fraction define in radioactive elements ( fraction of the total mass of the assembly )
    HashMap<ResourceLocation, Couple<Float>> fissileCrossSection = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"), Couple.create(1f, 583f), //cross-section in barn
                    CROWNS.resource("u238"), Couple.create(0.3f, 0.0001f),
                    CROWNS.resource("p239"), Couple.create(2f, 748f)

            ));//for U235,U358 and Plutonium -> percentage of total mass
    HashMap<ResourceLocation, Float>         molarConcentration  = new HashMap<>(
            Map.of(
                    CROWNS.resource("u235"), 19 / 235f * 10000, //amount of moles in a cubic meter of pure metal
                    CROWNS.resource("u238"), 19 / 238f * 10000,
                    CROWNS.resource("p239"), 19 / 239f * 10000

            ));//for U235,U358 and Plutonium -> percentage of total mass

    /**
     * @param radiationFlux the incoming flux
     * @return the out coming flux
     */
    Couple<Float> absorbNeutrons(Couple<Float> radiationFlux);

    float getEffectiveK();
}
