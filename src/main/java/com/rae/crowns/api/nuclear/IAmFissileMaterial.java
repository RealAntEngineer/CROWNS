package com.rae.crowns.api.nuclear;

import com.simibubi.create.foundation.utility.Couple;

public interface IAmFissileMaterial {
    // change that to a couple -> get the flux of neutron (fast,slow)
    // and return the amount transmitted (fast,slow)
    Couple<Float> absorbNeutrons(Couple<Float> radiationFlux);
}
