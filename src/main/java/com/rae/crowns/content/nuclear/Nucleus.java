package com.rae.crowns.content.nuclear;

import com.rae.crowns.config.CROWNSConfigs;
import net.createmod.catnip.data.Couple;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.rae.crowns.Constants.barnNa;

/**
 * Represents an atomic nucleus with its physical properties, including neutron absorption
 * behavior, radioactive decay, and nuclear transformation capabilities.
 *
 * <p>Each {@code Nucleus} is uniquely identified by its atomic mass and registered in a
 * static registry upon creation. No two nuclei may share the same atomic mass.</p>
 *
 * <p>Three kinds of nuclei can be modeled:</p>
 * <ul>
 *   <li><b>Stable</b> — does not decay and does not absorb neutrons.</li>
 *   <li><b>Radioactive</b> — decays over time according to a {@link NuclearEquation} and a half-life.</li>
 *   <li><b>Neutron-absorbing</b> — reacts upon neutron capture, then optionally decays.</li>
 * </ul>
 */
public class Nucleus {
    /**
     * Global registry mapping atomic mass → {@code Nucleus} instance.
     */
    private static final HashMap<Integer, Nucleus> VALUES = new HashMap<>();

    /**
     * Unique identifier for this nucleus within the {@link #VALUES} registry.
     * Defaults to {@link #atomicMass} unless explicitly overridden at construction time.
     * Use a custom id when multiple nuclei share the same atomic mass (e.g. delayed neutron
     * precursor groups DN1–DN6, which are abstract decay groups rather than real isotopes).
     */
    private final int           id;
    /**
     * Neutron cross-sections for this nucleus.
     * The first value of the {@link Couple} is the <b>fast</b> neutron cross-section,
     * the second is the <b>thermal</b> neutron cross-section.
     */
    private final Couple<Float> neutronCrossSections;

    /**
     * Total number of nucleons (protons + neutrons) in the nucleus.
     * Serves as the unique identifier in the {@link #VALUES} registry.
     */
    private final int atomicMass;
    /**
     * Number of proton inside the nucleus.
     * Must be ≥ 0.
     */
    private final int atomicNumber;
    /**
     * The {@link NuclearEquation} triggered <em>immediately</em> when this nucleus
     * captures a neutron. Provides instant transformation products.
     * Use {@link NuclearEquation#EMPTY} if the nucleus does not absorb neutrons, if it "burned".
     */
    private final NuclearEquation absorptionEquation;
    /**
     * The {@link NuclearEquation} triggered over time as this nucleus undergoes
     * radioactive decay. Evaluated against the current amount and elapsed time.
     * Use {@link NuclearEquation#EMPTY} for stable nuclei or if it "burned".
     */
    private final NuclearEquation decayEquation;
    /**
     * Time required for half of a sample of this nucleus to decay, expressed in
     * game ticks. {@link Float#MAX_VALUE} indicates a stable (non-decaying) nucleus.
     */
    private final float           halfLife;

// --- stable ---

    /**
     * @see #Nucleus(int, int, int)
     */
    public Nucleus(short mass, short number) {
        this(mass | number << 16, mass, number);
    }

    /**
     * Creates a <b>stable</b> nucleus with an explicit registry id.
     *
     * @param id     unique registry key; use when {@code mass} would collide with an existing nucleus
     * @param mass   total number of nucleons (protons + neutrons)
     * @param number number of proton; must be ≥ 0
     * @throws IllegalArgumentException if {@code number} is negative or {@code id} is already registered
     */
    public Nucleus(int id, int mass, int number) {
        this(id, mass, number, NuclearEquation.EMPTY, Float.MAX_VALUE);
    }
// --- radioactive ---

    /**
     * Creates a <b>radioactive</b> nucleus with an explicit registry id.
     *
     * @param id             unique registry key; use when {@code mass} would collide with an existing nucleus
     * @param mass           total number of nucleons (protons + neutrons)
     * @param number         number of proton; must be ≥ 0
     * @param decay_equation the {@link NuclearEquation} describing decay products and energy released
     * @param half_life      time for half the population to decay, in game ticks;
     *                       use {@link Float#MAX_VALUE} for effectively stable nuclei
     * @throws IllegalArgumentException if {@code number} is negative or {@code id} is already registered
     */
    public Nucleus(int id, int mass, int number, NuclearEquation decay_equation, float half_life) {
        this(id, mass, number, Couple.create(0f, 0f), NuclearEquation.EMPTY, decay_equation, half_life);
    }

    /**
     * Creates a fully-specified nucleus with an explicit registry id, capable of
     * <b>neutron absorption</b> and/or <b>radioactive decay</b>.
     *
     * @param id                   unique registry key; use when {@code mass} would collide with an existing nucleus
     * @param mass                 total number of nucleons (protons + neutrons)
     * @param number               number of neutrons; must be ≥ 0
     * @param neutronCrossSections a {@link Couple} of (fast σ, thermal σ) in barns
     * @param absorption_equation  the {@link NuclearEquation} applied instantly upon neutron capture;
     *                             use {@link NuclearEquation#EMPTY} if no immediate products
     * @param decay_equation       the {@link NuclearEquation} applied over time as the nucleus decays;
     *                             use {@link NuclearEquation#EMPTY} for stable nuclei
     * @param half_life            time for half the population to decay, in game ticks;
     *                             use {@link Float#MAX_VALUE} for effectively stable nuclei
     * @throws IllegalArgumentException if {@code number} is negative or {@code id} is already registered
     */
    public Nucleus(int id, int mass, int number, Couple<Float> neutronCrossSections,
                   NuclearEquation absorption_equation, NuclearEquation decay_equation, float half_life) {
        this.absorptionEquation = absorption_equation;
        this.decayEquation = decay_equation;
        this.halfLife = half_life;
        if (number < 0) {
            throw new IllegalArgumentException("Number must be greater than zero");
        } else if (VALUES.containsKey(id)) {
            throw new IllegalArgumentException("Id already taken: " + id + ", registry ids must be unique");
        } else {
            this.id = id;
            this.neutronCrossSections = neutronCrossSections;
            this.atomicMass = mass;
            this.atomicNumber = number;
            VALUES.put(this.id, this);
        }
    }
// --- neutron absorbing ---

    /**
     * @see #Nucleus(int, int, int, NuclearEquation, float)
     */
    public Nucleus(int mass, int number, NuclearEquation decay_equation, float half_life) {
        this(mass, mass, number, Couple.create(0f, 0f), NuclearEquation.EMPTY, decay_equation, half_life);
    }

    /**
     * @see #Nucleus(int, int, int, Couple, NuclearEquation, NuclearEquation, float)
     */
    public Nucleus(int mass, int number, Couple<Float> neutronCrossSections,
                   NuclearEquation absorption_equation, NuclearEquation decay_equation, float half_life) {
        this(mass, mass, number, neutronCrossSections, absorption_equation, decay_equation, half_life);
    }

    /**
     * Computes the radioactive decay of this nucleus over a given time step.
     *
     * <p>The decayed advancement follows the standard radioactive decay law:</p>
     * <pre>
     *   λ           = ln(2) / halfLife
     *   advancement = amount × (1 − e^(−λ × time))
     * </pre>
     * <p>This represents the quantity of nucleus that has actually transformed during
     * the time step, which is then forwarded to the {@link #decayEquation}.
     * For stable nuclei ({@link #halfLife} == {@link Float#MAX_VALUE}),
     * the exponent approaches zero and advancement will be effectively {@code 0}.</p>
     *
     * @param time   elapsed time in game ticks; must be ≥ 0
     * @param amount current quantity of this nucleus (e.g. in moles); must be ≥ 0
     * @return a {@link NuclearTransformationResult} describing the daughter products,
     * neutrons emitted, and energy released for this time step
     */
    public @NotNull NuclearTransformationResult decay(float time, float amount) {
        float lambda      = (float) (Math.log(2) / halfLife);
        float advancement = (float) (amount * (1 - Math.exp(-lambda * time)));
        return decayEquation.compute(advancement);
    }

    /**
     * Computes the neutron absorption and resulting nuclear transformation for this nucleus.
     *
     * @param neutrons incoming neutron count (fast or thermal depending on {@code fast})
     * @param amount   quantity of this nucleus present, in moles
     * @param volume   volume occupied by the material, in m³
     * @param depth    thickness of the material along the neutron path, in meters
     * @param fast     {@code true} to use the fast-neutron cross-section;
     *                 {@code false} to use the thermal-neutron cross-section
     * @return a {@link NuclearTransformationResult} describing the daughter products,
     * neutrons emitted, and energy released; never {@code null}
     */
    public @NotNull NuclearTransformationResult fission(float neutrons, float amount, float volume, float depth, boolean fast) {
        float surface     = volume / depth;
        float neutronFlux = neutrons / surface;
        float sigma       = getNeutronCrossSections(fast);
        float c           = amount / volume;
        float absorbed    = c * neutronFlux * sigma * barnNa;//it's from wikipedia but I'm not convinced
        return absorptionEquation.compute(absorbed);
    }

    /**
     * Returns the neutron cross-section (σ) for this nucleus, in <b>barns</b>.
     *
     * <p>Intended to be multiplied by the material's mass fraction, molar concentration,
     * and the barn-to-cm² × Avogadro constant ({@code barnNa}) to yield an absorption probability:</p>
     * <pre>
     *   absorptionChance = clamp(σ × massFraction × molarConcentration × barnNa, 0, 1)
     * </pre>
     *
     * @param fast {@code true} for the <b>fast</b>-neutron cross-section;
     *             {@code false} for the <b>thermal</b>-neutron cross-section
     * @return the cross-section in barns; never {@code null}
     */
    public @NotNull Float getNeutronCrossSections(boolean fast) {
        return neutronCrossSections.get(fast);
    }

    /**
     *
     * @param mole number of moles
     * @return mass in Kg
     */
    public float moleToMass(float mole) {
        return mole * atomicMass / 1000;
    }

    /**
     *
     * @param mass mass in Kg
     * @return the number of moles
     */
    public float massToMole(float mass) {
        return mass / atomicMass * 1000;
    }

    public int getAtomicNumber() {
        return atomicNumber;
    }


    public int getAtomicMass() {
        return atomicMass;
    }

    public int getId() {
        return id;
    }

    /**
     * Describes a nuclear reaction as a stoichiometric equation: a set of product nuclei
     * with their per-unit yields, the number of neutrons emitted, and the energy released.
     *
     * <p>A {@code NuclearEquation} is stateless and reusable. It is evaluated by calling
     * {@link #compute(float)} with a reaction <em>advancement</em> value).</p>
     *
     * @param element_map     mapping of atomic-mass/id integer → stoichiometric coefficient.
     *                        Each key must correspond to a nucleus registered in {@link #VALUES}.
     *                        Coefficients are multiplied by the advancement to obtain absolute quantities.
     * @param neutron_yielded number of neutrons produced per mole of advancement.
     *                        Scaled by the server's {@code neutronFluxMultiplicator} at evaluation time.
     * @param energy_yielded  energy released per mole of advancement (J).
     */
    public record NuclearEquation(Map<Integer, Float> element_map, float neutron_yielded, float energy_yielded) {
        /**
         * A no-op {@code NuclearEquation} representing the absence of any nuclear reaction.
         * Produces no products, no neutrons, and no energy. Used for stable nuclei and
         * nuclei that do not interact with neutrons or for nuclei that burn
         */
        public static final Nucleus.NuclearEquation EMPTY = new Nucleus.NuclearEquation(Map.of(), 0f, 0f);

        /**
         * Evaluates this equation for a given reaction advancement and returns the
         * resulting transformation.
         *
         * <p>Each product nucleus quantity is computed as {@code coefficient × advancement}.
         * Neutron yield is additionally multiplied by the server-side
         * {@code neutronFluxMultiplicator} config value.</p>
         *
         * @param advancement the extent of reaction (typically {@code amount × time}); must be ≥ 0
         * @return a {@link NuclearTransformationResult} containing the scaled products,
         * neutron count, and total energy; never {@code null}
         */
        public @NotNull NuclearTransformationResult compute(float advancement) {
            Map<Nucleus, Float> elements = new HashMap<>();
            element_map.forEach((element, quantity) -> elements.put(VALUES.get(element), quantity * advancement));
            return new NuclearTransformationResult(elements, neutron_yielded * advancement * CROWNSConfigs.SERVER.nuclear.neutronFluxMultiplicator.getF(),
                    energy_yielded * advancement, advancement);
        }

    }

    /**
     * Immutable snapshot of the outcome of a nuclear transformation (absorption or decay).
     *
     * <p>Instances are produced by {@link NuclearEquation#compute(float)} and consumed by the
     * simulation to update the reactor's inventory, neutron flux, and heat generation.</p>
     *
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * NuclearTransformationResult result = nucleus.decay(deltaTick, currentAmount);
     *
     * // Add daughter products to the reactor inventory
     * result.elements().forEach((daughterNucleus, quantity) -> inventory.add(daughterNucleus, quantity));
     *
     * // Inject free neutrons into the neutron flux
     * neutronFlux += result.neutron_yielded();
     *
     * // Convert energy to heat
     * heatBuffer += result.energy_yielded();
     * }</pre>
     *
     * @param elements        map of daughter {@link Nucleus} instances to their produced quantities
     *                        (in the same unit as the advancement passed to {@link NuclearEquation#compute}).
     *                        may be empty for reactions that produce no daughter nuclei
     *                        (e.g. pure energy release or neutron emission only).
     * @param neutron_yielded total number of free neutrons emitted by this transformation.
     *                        Already scaled by the server's {@code neutronFluxMultiplicator}.
     *                        A value of {@code 0} means no neutrons were released.
     * @param energy_yielded  total energy released by this transformation in game energy units.
     *                        A positive value indicates exothermic reaction (energy released as heat);
     *                        a negative value would indicate an endothermic reaction, though this is
     *                        not expected in normal fission/decay scenarios.
     */
    public record NuclearTransformationResult(Map<Nucleus, Float> elements, float neutron_yielded,
                                              float energy_yielded, float consumed) {
    }
}
