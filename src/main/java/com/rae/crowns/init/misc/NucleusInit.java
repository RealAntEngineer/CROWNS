package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.Nucleus;
import net.createmod.catnip.data.Couple;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class NucleusInit {

    static long Day = 24000L;
    static long Year = 31_556_952L * 20;

    @NotNull public static Nucleus Sr90 = new Nucleus(90, 90, 38, Nucleus.NuclearEquation.EMPTY, 2.8f * Day);
    @NotNull public static Nucleus Zr92 = new Nucleus(93, 92, 52);
    @NotNull public static Nucleus Xe135 = new Nucleus(135, 54, Couple.create(7f, 7f),
            Nucleus.NuclearEquation.EMPTY,
            Nucleus.NuclearEquation.EMPTY, 1200);
    @NotNull public static Nucleus Cs137 = new Nucleus(137, 137, 55, Nucleus.NuclearEquation.EMPTY, 3f * Day);
    @NotNull public static Nucleus Nd144 = new Nucleus(144, 60);
    @NotNull public static Nucleus Sm149 = new Nucleus(149, 62);
    @NotNull public static Nucleus Be9 = new Nucleus(9, 9, 4); // It's just a thing

    // Delayed neutron precursor groups (DN1..DN6)
    @NotNull public static Nucleus DN1 = new Nucleus(
            8001, 0, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f), // emits 1 neutron
            55.6f * 20f // ~1112 ticks
    );

    @NotNull public static Nucleus DN2 = new Nucleus(
            8002, 0, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            22.7f * 20f // ~454 ticks
    );

    @NotNull public static Nucleus DN3 = new Nucleus(
            8003, 0, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            6.2f * 20f // ~454 ticks
    );
    @NotNull public static Nucleus DN4 = new Nucleus(
            8004, 0, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            2.3f * 20f // ~454 ticks
    );
    @NotNull public static Nucleus DN5 = new Nucleus(
            8005, 0, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            0.61f * 20f // ~454 ticks
    );
    @NotNull public static Nucleus DN6 = new Nucleus(
            8006, 0, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            0.23f * 20f // ~454 ticks
    );

    // U-235 with prompt + delayed neutrons
    @NotNull public static Nucleus U235 = new Nucleus(
            235, 92,
            Couple.create(1f, 583f),
            new Nucleus.NuclearEquation(
                    Map.ofEntries(
                            entry(135, 0.06f),   // Xe-135
                            entry(137, 0.06f),   // Cs-137
                            entry(90, 0.06f),    // Sr-90
                            entry(149, 0.011f),  // Sm-149
                            entry(92, 0.06f),    // Zr-92
                            entry(144, 0.05f),   // Nd-144
                            entry(8001, 0.000215f), // DN1
                            entry(8002, 0.001424f), // DN2
                            entry(8003, 0.001274f), // DN3
                            entry(8004, 0.002568f), // DN4
                            entry(8005, 0.000748f), // DN5
                            entry(8006, 0.000273f)  // DN6
                    ),
                    2.39f, // prompt neutrons only
                    19.54f * 1e12f
            ),
            new Nucleus.NuclearEquation(Map.of(231, 1f), 0f, 0f),
            1000f * Day
    );

    // Pu-239 with prompt + delayed neutrons
    @NotNull public static Nucleus Pu239 = new Nucleus(
            239, 239, 94,
            Couple.create(10f, 742f),
            new Nucleus.NuclearEquation(
                    Map.ofEntries(
                            entry(141, 0.06f),   // Ba-141
                            entry(92, 0.06f),    // Kr-92
                            entry(135, 0.03f),   // Xe-135
                            entry(137, 0.03f),   // Cs-137
                            entry(90, 0.03f),    // Sr-90
                            entry(149, 0.008f),  // Sm-149
                            entry(93, 0.02f),    // Zr-92
                            entry(144, 0.01f),   // Nd-144
                            entry(8001, 0.000105f), // DN1
                            entry(8002, 0.000924f), // DN2
                            entry(8003, 0.001274f), // DN3
                            entry(8004, 0.000568f), // DN4
                            entry(8005, 0.000748f), // DN5
                            entry(8006, 0.000273f)  // DN6
                    ),
                    2.9f, // prompt neutrons only
                    20.07f * 1e12f
            ),
            new Nucleus.NuclearEquation(Map.of(235, 1f), 0f, 0f),
            100f * Day
    );

    // Np237, similar to U235 and Pu239
    @NotNull public static Nucleus Np237 = new Nucleus(
            237, 93,
            Couple.create(2.8f, 0.001f),
            new Nucleus.NuclearEquation(
                    Map.ofEntries(
                            entry(135, 0.06f),   // Xe-135
                            entry(137, 0.06f),   // Cs-137
                            entry(90, 0.06f),    // Sr-90
                            entry(149, 0.011f),  // Sm-149
                            entry(92, 0.06f),    // Zr-92
                            entry(144, 0.05f),   // Nd-144
                            entry(8001, 0.0000215f), // DN1
                            entry(8002, 0.0001424f), // DN2
                            entry(8003, 0.0001274f), // DN3
                            entry(8004, 0.0002568f), // DN4
                            entry(8005, 0.0000748f), // DN5
                            entry(8006, 0.0000273f)  // DN6
                    ),
                    2.39f, // prompt neutrons only
                    19.54f * 1e12f
            ),
            Nucleus.NuclearEquation.EMPTY,
            800f * Day
    );

    @NotNull public static Nucleus U236 = new Nucleus(236, 92,
            new Nucleus.NuclearEquation(Map.of(92, 1f, 141, 1f), 3, 0f), 1);

    @NotNull public static Nucleus U238 = new Nucleus(238, 92, Couple.create(108f, 2.7f),
            new Nucleus.NuclearEquation(Map.of(1001, 0.1f), 0, 0f),
            new Nucleus.NuclearEquation(Map.of(234, 1f), 0, 0f), 1000f * Day);

    @NotNull public static Nucleus U239 = new Nucleus(1001, 239, 92,
            new Nucleus.NuclearEquation(Map.of(1002, 1f), 0, 0f), 1692f);

    @NotNull public static Nucleus Np239 = new Nucleus(1002, 239, 93,
            new Nucleus.NuclearEquation(Map.of(239, 1f), 0, 0f), Day);

    @NotNull public static Nucleus Am241Be = new Nucleus(241, 241, 95, new Nucleus.NuclearEquation(Map.of(237, 1f), 10f, 8.88e-13f), 100f * Day); // Exagerated for noticable effect

    @NotNull public static Nucleus Cf252 = new Nucleus(252, 252, 98,
            new Nucleus.NuclearEquation(
                Map.ofEntries(
                        entry(146, 0.06f),
                        entry(106, 0.07f)
                ),
                3.7f,
                18.72f * 1e12f
            ),
            2f * Day
    );

    @NotNull public static Nucleus Ba146 = new Nucleus(146, 146, 56, Nucleus.NuclearEquation.EMPTY, 2.16f * 20);

    @NotNull public static Nucleus Mo106 = new Nucleus(106, 106, 42, Nucleus.NuclearEquation.EMPTY, 8.73f * 20);

    @NotNull public static Nucleus Ba141 = new Nucleus(141, 141, 56, Nucleus.NuclearEquation.EMPTY, 1207f);

    @NotNull public static Nucleus Kr92 = new Nucleus(92, 92, 36, Nucleus.NuclearEquation.EMPTY, 1.841f * 20f);

    public static final List<Nucleus> allNuclei = List.of(
            Sr90,
            Zr92,
            Xe135,
            Cs137,
            Nd144,
            Sm149,
            DN1,
            DN2,
            DN3,
            DN4,
            DN5,
            DN6,
            U235,
            Pu239,
            U236,
            U238,
            Am241Be,
            Np237,
            Be9,
            Cf252,
            Ba146,
            Mo106,
            Ba141,
            Kr92,
            U239,
            Np239
    ); // I don't like this, but it works for now
}