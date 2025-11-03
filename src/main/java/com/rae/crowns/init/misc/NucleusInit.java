package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.Nucleus;
import net.createmod.catnip.data.Couple;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.util.Map.entry;

public class NucleusInit {

    long Day = 24000L;

    @NotNull Nucleus Sr90 = new Nucleus(90, 38);
    @NotNull Nucleus Zr92 = new Nucleus(92, 52);
    @NotNull Nucleus Xe135 = new Nucleus(135, 54, Couple.create(7f, 7f),
            Nucleus.NuclearEquation.EMPTY,
            Nucleus.NuclearEquation.EMPTY, Day * 0.5f);
    @NotNull Nucleus Cs137 = new Nucleus(137, 55, new Nucleus.NuclearEquation(Map.of(), 1, 0f),
            30 * Day);
    @NotNull Nucleus Nd144 = new Nucleus(144, 60);
    @NotNull Nucleus Sm149 = new Nucleus(149, 62);

    // Delayed neutron precursor groups (DN1..DN6)
    @NotNull Nucleus DN1 = new Nucleus(
            8001, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f), // emits 1 neutron
            55.6f * 20f // ~1112 ticks
    );

    @NotNull Nucleus DN2 = new Nucleus(
            8002, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            22.7f * 20f // ~454 ticks
    );

    @NotNull Nucleus DN3 = new Nucleus(
            8002, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            6.2f * 20f // ~454 ticks
    );
    @NotNull Nucleus DN4 = new Nucleus(
            8002, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            2.3f * 20f // ~454 ticks
    );
    @NotNull Nucleus DN5 = new Nucleus(
            8002, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            0.61f * 20f // ~454 ticks
    );
    @NotNull Nucleus DN6 = new Nucleus(
            8002, 0,
            new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            0.23f * 20f // ~454 ticks
    );

// ... repeat for DN3–DN6 with their half-lives

    // U-235 with prompt + delayed neutrons
    @NotNull Nucleus U235 = new Nucleus(
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
            100f * Day
    );

    @NotNull Nucleus U236 = new Nucleus(236, 92,
            new Nucleus.NuclearEquation(Map.of(92, 1f, 141, 1f), 3, 0f), 1);

    @NotNull Nucleus U238 = new Nucleus(238, 92, Couple.create(0.3f, 0.0001f),
            new Nucleus.NuclearEquation(Map.of(236, 0.1f), 0, 0f),
            new Nucleus.NuclearEquation(Map.of(234, 1f), 0, 0f), 100f * Day);


}