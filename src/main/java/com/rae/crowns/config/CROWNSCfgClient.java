package com.rae.crowns.config;

import com.rae.crowns.CROWNS;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSCfgClient extends ConfigBase {


    @Override
    public @NotNull String getName() {
        return CROWNS.MODID +".client";
    }

    private static class Comments {
        static String units = "Units used";

    }

}
