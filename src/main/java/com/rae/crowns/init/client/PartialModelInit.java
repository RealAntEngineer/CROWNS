package com.rae.crowns.init.client;

import com.rae.crowns.CROWNS;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

@SuppressWarnings("ALL")
public class PartialModelInit {
    public static final PartialModel TURBINE_STAGE = block("turbine/turbine_stage");


    private static PartialModel block(String path) {
        return PartialModel.of(CROWNS.resource("block/" + path));
    }

    public static void init() {
        // init static fields
    }
}
