package com.rae.crowns.init;

import com.rae.crowns.CROWNS;
import com.rae.crowns.content.legacy.TurbineContraption;
import com.simibubi.create.content.contraptions.ContraptionType;


public class CROWNSContraptionType {
    public static final ContraptionType TURBINE = ContraptionType.register(CROWNS.resource("rocket").toString(), TurbineContraption::new);

    public CROWNSContraptionType() {
    }

    public static void prepare() {
    }
}
