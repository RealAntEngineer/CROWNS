package com.rae.crowns.init.misc;

import com.rae.crowns.content.nuclear.Nucleus;
import net.createmod.catnip.data.Couple;
import org.lwjgl.system.NonnullDefault;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@NonnullDefault
public class NucleusInit {

    static long Day = 24000L;
    static long Year = 31_556_952L * 20;
    public static Nucleus Be9;
    public static Nucleus Sr90;
    public static Nucleus Zr92;
    public static Nucleus Kr92;
    public static Nucleus Xe135;
    public static Nucleus Cs137;
    public static Nucleus Ba141;
    public static Nucleus Nd144;
    public static Nucleus Sm149;
    public static Nucleus Th231;
    public static Nucleus Th234;
    public static Nucleus U236;
    public static Nucleus U238;
    public static Nucleus U239;
    public static Nucleus Np239;

    static {
        Sr90 = new Nucleus(90, 38, () -> Nucleus.NuclearEquation.EMPTY, 2.8f * Day);
        Zr92 = new Nucleus((short) 92, (short) 52);
        Xe135 = new Nucleus(135, 54, Couple.create(7f, 7f),
                () -> Nucleus.NuclearEquation.EMPTY,
                () -> Nucleus.NuclearEquation.EMPTY, 1200);
        Cs137 = new Nucleus(137, 55, () -> Nucleus.NuclearEquation.EMPTY, 3f * Day);
        Nd144 = new Nucleus((short) 144, (short) 60);
        Sm149 = new Nucleus((short) 149, (short) 62);
        Be9   = new Nucleus((short) 9, (short) 4); // It's just a thing
    }
    // Delayed neutron precursor groups (DN1..DN6)
    public static Nucleus DN1 = new Nucleus(
            8001, 0, 0,
            () -> new Nucleus.NuclearEquation(Map.of(), 1f, 0f), // emits 1 neutron
            55.6f * 20f // ~1112 ticks
    );

    public static Nucleus DN2 = new Nucleus(
            8002, 0, 0,
            () -> new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            22.7f * 20f // ~454 ticks
    );

    public static Nucleus DN3 = new Nucleus(
            8003, 0, 0,
            () -> new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            6.2f * 20f // ~454 ticks
    );
    public static Nucleus DN4 = new Nucleus(
            8004, 0, 0,
            () -> new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            2.3f * 20f // ~454 ticks
    );
    public static Nucleus DN5 = new Nucleus(
            8005, 0, 0,
            () -> new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            0.61f * 20f // ~454 ticks
    );
    public static Nucleus DN6 = new Nucleus(
            8006, 0, 0,
            () -> new Nucleus.NuclearEquation(Map.of(), 1f, 0f),
            0.23f * 20f // ~454 ticks
    );


    static {
        Th231 = new Nucleus((short) 231, (short) 90);
        Th234 = new Nucleus((short) 234, (short) 90);
    }
    // U-235 with prompt + delayed neutrons
    public static Nucleus U235 = new Nucleus(235, 92,
            Couple.create(1f, 583f),
            () -> new Nucleus.NuclearEquation(
                    Map.ofEntries(
                            entry(Xe135, 0.06f),   // Xe-135
                            entry(Cs137, 0.06f),   // Cs-137
                            entry(Sr90, 0.06f),    // Sr-90
                            entry(Sm149, 0.011f),  // Sm-149
                            entry(Zr92, 0.06f),    // Zr-92
                            entry(Nd144, 0.05f),   // Nd-144
                            entry(DN1, 0.000215f), // DN1
                            entry(DN2, 0.001424f), // DN2
                            entry(DN3, 0.001274f), // DN3
                            entry(DN4, 0.002568f), // DN4
                            entry(DN5, 0.000748f), // DN5
                            entry(DN6, 0.000273f)  // DN6
                    ),
                    2.39f, // prompt neutrons only
                    19.54f * 1e12f
            ),
            () -> new Nucleus.NuclearEquation(Map.of(Th231, 1f), 0f, 0f),
            1000f * Day
    );

    // Pu-239 with prompt + delayed neutrons
    public static Nucleus Pu239 = new Nucleus(239, 94,
            Couple.create(10f, 742f),
            () -> new Nucleus.NuclearEquation(
                    Map.ofEntries(
                            entry(Ba141, 0.06f),   // Ba-141
                            entry(Kr92, 0.06f),    // Kr-92
                            entry(Xe135, 0.03f),   // Xe-135
                            entry(Cs137, 0.03f),   // Cs-137
                            entry(Sr90, 0.03f),    // Sr-90
                            entry(Sm149, 0.008f),  // Sm-149
                            entry(Zr92, 0.02f),    // Zr-92
                            entry(Nd144, 0.01f),   // Nd-144
                            entry(DN1, 0.000105f), // DN1
                            entry(DN2, 0.000924f), // DN2
                            entry(DN3, 0.001274f), // DN3
                            entry(DN4, 0.000568f), // DN4
                            entry(DN5, 0.000748f), // DN5
                            entry(DN6, 0.000273f)  // DN6
                    ),
                    2.9f, // prompt neutrons only
                    20.07f * 1e12f
            ),
            () -> new Nucleus.NuclearEquation(Map.of(U235, 1f), 0f, 0f),
            100f * Day
    );
    static {
        Kr92 = new Nucleus(92, 92, 36, () ->Nucleus.NuclearEquation.EMPTY, 1.841f * 20f);
    }
    // Np237, similar to U235 and Pu239
    public static Nucleus Np237 = new Nucleus(237, 93,
            Couple.create(280f, 0.001f),
            () -> new Nucleus.NuclearEquation(
                    Map.ofEntries(
                            entry(Ba141, 0.06f),   // Ba-141
                            entry(Kr92, 0.06f),    // Kr-92
                            entry(Xe135, 0.03f),   // Xe-135
                            entry(Cs137, 0.03f),   // Cs-137
                            entry(Sr90, 0.03f),    // Sr-90
                            entry(Sm149, 0.008f),  // Sm-149
                            entry(Zr92, 0.02f),    // Zr-92
                            entry(Nd144, 0.01f),   // Nd-144
                            entry(DN1, 0.000105f), // DN1
                            entry(DN2, 0.000924f), // DN2
                            entry(DN3, 0.001274f), // DN3
                            entry(DN4, 0.000568f), // DN4
                            entry(DN5, 0.000748f), // DN5
                            entry(DN6, 0.000273f)  // DN6
                    ),
                    2.39f, // prompt neutrons only
                    19.54f * 1e12f
            ),
            () -> Nucleus.NuclearEquation.EMPTY,
            800f * Day
    );

    static {
        U236 = new Nucleus(236, 92,
                () -> new Nucleus.NuclearEquation(Map.of(Kr92, 1f, Ba141, 1f), 3, 0f), 1);

        U238 = new Nucleus(238, 92, Couple.create(108f, 2.7f),
                () -> new Nucleus.NuclearEquation(Map.of(U239, 0.1f), 0, 0f),
                () -> new Nucleus.NuclearEquation(Map.of(Th234, 1f), 0, 0f), 1000f * Day);

        U239 = new Nucleus(239, 92,
                () -> new Nucleus.NuclearEquation(Map.of(Np239, 1f), 0, 0f), 1692f);

        Np239 = new Nucleus(239, 93,
                () -> new Nucleus.NuclearEquation(Map.of(U239, 1f), 0, 0f), Day);
    }
    public static Nucleus Am241Be = new Nucleus(241, 95, () -> new Nucleus.NuclearEquation(Map.of(Np237, 1f), 10f, 8.88e-13f), 100f * Day); // Exagerated for noticable effect


    public static Nucleus Ba146 = new Nucleus(146, 56, () -> Nucleus.NuclearEquation.EMPTY, 2.16f * 20);

    public static Nucleus Mo106 = new Nucleus(106, 42, () -> Nucleus.NuclearEquation.EMPTY, 8.73f * 20);

    public static Nucleus Cf252 = new Nucleus(252, 98,
            () -> new Nucleus.NuclearEquation(
                Map.ofEntries(
                        entry(Ba146, 0.06f),
                        entry(Mo106, 0.07f)
                ),
                3.7f,
                18.72f * 1e12f
            ),
            2f * Day
    );

    static {
        Ba141 = new Nucleus(141, 56, () -> Nucleus.NuclearEquation.EMPTY, 1207f);
    }

    //TODO just add a getter to VALUES
    /*public static final List<Nucleus> allNuclei = List.of(
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
    ); // I don't like this, but it works for now*/
}