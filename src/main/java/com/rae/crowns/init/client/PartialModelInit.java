package com.rae.crowns.init.client;

import com.rae.crowns.CROWNS;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

@SuppressWarnings("ALL")
public class PartialModelInit {
    public static final PartialModel TURBINE_STAGE = block("turbine_stage/rotor");
    public static final PartialModel GOLD_ROD = block("rods/gold_rod");
    public static final PartialModel BORON_ROD = block("rods/boron_rod");
    public static final PartialModel GRAPHITE_ROD = block("rods/graphite_rod");

    private static PartialModel block(String path) {
        return PartialModel.of(CROWNS.resource("block/" + path));
    }

    public static void init() {
        // init static fields
    }
}