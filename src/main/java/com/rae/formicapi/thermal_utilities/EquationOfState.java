package com.rae.formicapi.thermal_utilities;

public interface EquationOfState {
    //TODO we only wants to use the
    /**
     * @param temperature temperature in Kelvin
     * @param volumeMolar molar volume in m^3/mol
     * @return pressure in Pascals
     */
    double pressure(double temperature, double volumeMolar);

    /**
     * @param temperature temperature in Kelvin
     * @param pressure pressure in Pascals
     * @param vaporFraction fraction of vapor bwn 0 and 1
     * @return molar volume in m^3/mol
     */
    default double volumeMolar(double temperature, double pressure, double vaporFraction) {
        throw new UnsupportedOperationException("Not implemented");
    }



}
