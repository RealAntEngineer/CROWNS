package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;

import static net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack.FLUID_NBT_KEY;

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
