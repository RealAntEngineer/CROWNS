package com.rae.crowns.content.nuclear;

import com.rae.crowns.config.CROWNSConfigs;
import net.createmod.catnip.data.Couple;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * neutronCrossSection:
 */
public class Nucleus {
    private static final HashMap<Integer, Nucleus> VALUES = new HashMap<>();
    private final Couple<Float> neutronCrossSections;
    private final int atomic_mass;
    private final int atomic_number;
    private final NuclearEquation absorptionEquation;
    private final NuclearEquation decayEquation;
    private final float halfLife;

    /**
     * stable Nucleus
     *
     * @param mass   : atomic mass (neutrons + protons inside the nucleus)
     * @param number : atomic number (number of neutrons)
     */
    public Nucleus(int mass, int number) {
        this(mass, number, NuclearEquation.EMPTY, Float.MAX_VALUE);
    }

    /**
     * radioactive Nucleus
     *
     * @param mass           : atomic mass (neutrons + protons inside the nucleus)
     * @param number         : atomic number (number of neutrons)
     * @param decay_equation : equation run depending on the half life.
     * @param half_life      : how much time it takes for half of the nucleus to decay, in ticks
     */
    public Nucleus(int mass, int number, NuclearEquation decay_equation, float half_life) {
        this(mass, number, Couple.create(0f, 0f), NuclearEquation.EMPTY, decay_equation, half_life);
    }

    /**
     * neutron absorbing Nucleus
     *
     * @param mass                 : atomic mass (neutrons + protons inside the nucleus)
     * @param number               : atomic number (number of neutrons)
     * @param neutronCrossSections : cross-section for fast | thermal neutrons
     * @param absorption_equation  : equation run through when hit by a neutron, this is here to provide instant result,
     *                             delay results will be made with the decay equation.
     * @param decay_equation       : equation run depending on the half life.
     * @param half_life            : how much time it takes for half of the nucleus to decay, in ticks
     */
    public Nucleus(int mass, int number, Couple<Float> neutronCrossSections,
                   NuclearEquation absorption_equation, NuclearEquation decay_equation, float half_life) {
        this.absorptionEquation = absorption_equation;
        decayEquation = decay_equation;
        halfLife = half_life;
        if (number < 0) {
            throw new IllegalArgumentException("Number must be greater than zero");
        } else if (VALUES.containsKey(mass)) {
            throw new IllegalArgumentException("Atomic mass already taken : " + mass + " number of nucleons must be unique");
        } else {
            this.neutronCrossSections = neutronCrossSections;
            this.atomic_mass = mass;
            this.atomic_number = number;
            VALUES.put(this.atomic_mass, this);
        }
    }

    public @NotNull Float getNeutronCrossSections(boolean fast) {
        return neutronCrossSections.get(fast);
    }

    /**
     * @param element_map    implicitly all the elements are products.
     * @param energy_yielded energy yielded for 1 mole
     */
    public record NuclearEquation(Map<Integer, Float> element_map, float neutron_yielded, float energy_yielded) {
        public static final Nucleus.NuclearEquation EMPTY = new Nucleus.NuclearEquation(Map.of(), 0f, 0f);

        public @NotNull NuclearTransformationResult compute(float advancement) {
            Map<Nucleus, Float> elements = new HashMap<>();
            element_map.forEach((element, quantity) -> elements.put(VALUES.get(element), quantity * advancement));
            return new NuclearTransformationResult(elements, neutron_yielded * advancement * CROWNSConfigs.SERVER.nuclear.easeCoef.getF(),
                    energy_yielded * advancement);
        }

    }

    public record NuclearTransformationResult(Map<Nucleus, Float> elements, float neutron_yielded,
                                              float energy_yielded) {
    }
}
