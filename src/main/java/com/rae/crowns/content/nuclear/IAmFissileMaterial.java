package com.rae.crowns.content.nuclear;


import net.createmod.catnip.data.Couple;

public interface IAmFissileMaterial {
    // change that to a couple -> get the flux of neutron (fast,slow)
    // and return the amount transmitted (fast,slow)
    Couple<Float> absorbNeutrons(Couple<Float> radiationFlux);
    float getEffectiveK();
}
