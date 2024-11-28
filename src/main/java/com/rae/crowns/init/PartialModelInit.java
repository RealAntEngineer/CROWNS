package com.rae.crowns.init;

import com.jozufozu.flywheel.core.PartialModel;
import com.rae.crowns.CROWNS;

public class PartialModelInit {
    public static final PartialModel TURBINE_STAGE = block("turbine/turbine_stage");

    private static PartialModel block(String path) {
        return new PartialModel(CROWNS.resource("block/" + path));
    }

    public static void init() {
        // init static fields
    }
}
