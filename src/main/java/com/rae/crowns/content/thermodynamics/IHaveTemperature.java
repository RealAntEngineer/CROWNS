package com.rae.crowns.content.thermodynamics;

/**
 * Represents a block entity that has a dynamic temperature state.
 * <p>
 * This interface should be implemented <strong>only</strong> by classes
 * extending {@link com.simibubi.create.foundation.blockEntity.SmartBlockEntity}.
 * It is used for entities whose temperature changes over time due to
 * simulation or interaction with the environment.
 * </p>
 *
 * <p>
 * Static temperature-related values for blocks, fluids, and biomes are instead
 * defined through datapack entries loaded via {@code FloatMapDataLoader}.
 * Those data-driven values represent base or environmental temperatures and
 * are used internally by the temperature system — they are not replaced by this
 * interface.
 * </p>
 *
 * <p>
 * Implementing {@code IHaveTemperature} allows a block entity to participate
 * in dynamic temperature updates and heat exchange within the world.
 * </p>
 */
public interface IHaveTemperature {

    /**
     * @return The thermal capacity of this block entity.
     *         Higher values indicate that more energy is required
     *         to change its temperature.
     */
    float getThermalCapacity();

    /**
     * @return The thermal conductivity of this block entity.
     *         Determines how efficiently it transfers heat
     *         to or from neighboring blocks or entities.
     */
    float getThermalConductivity();

    /**
     * @return The current temperature of this block entity,
     *         expressed in Kelvin or in the mod’s internal unit.
     */
    float getTemperature();

    /**
     * Applies a change in temperature to this block entity.
     *
     * @param dT The temperature delta to apply.
     *           Negative values decrease temperature (cooling),
     *           positive values increase it (heating).
     */
    void addTemperature(float dT);
}