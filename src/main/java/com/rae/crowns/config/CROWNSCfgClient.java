package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import com.simibubi.create.foundation.config.ConfigBase;

public class CROWNSCfgClient extends ConfigBase {


    public final CROWNSUnits units = nested(0, CROWNSUnits::new, Comments.units);

    @Override
    public String getName() {
        return CROWNS.MODID +".client";
    }

    private static class Comments {
        static String units = "Units used";
    }

}
