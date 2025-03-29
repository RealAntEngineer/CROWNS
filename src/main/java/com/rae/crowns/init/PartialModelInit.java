package com.rae.crowns.init;

import com.rae.crowns.CROWNS;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

@SuppressWarnings("ALL")
public class PartialModelInit {
    public static final PartialModel TURBINE_STAGE = block("turbine/turbine_stage");
    public static final PartialModel COMPRESSOR_STAGE = block("compressor/compressor_stage");


    private static PartialModel block(String path) {
        return PartialModel.of(CROWNS.resource("block/" + path));
    }

    public static void init() {
        // init static fields
    }
}
