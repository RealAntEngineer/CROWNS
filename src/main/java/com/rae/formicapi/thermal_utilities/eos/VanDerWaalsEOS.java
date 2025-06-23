package com.rae.formicapi.thermal_utilities.eos;

public class VanDerWaalsEOS implements EquationOfState {
    // Constants specific to the substance (e.g., water)
    private final double a; // [Pa·m^6/mol^2]
    private final double b; // [m^3/mol]
    private final double R = 8.314462618; // [J/mol·K]

    public VanDerWaalsEOS(double Pc, double Tc) {
        this.a = 27f/64*R*R*Tc*Tc/Pc;
        this.b = R*Tc/Pc/8;
        //System.out.println("VanDerWaalsEOS : "+a + " | " + b);
    }


    /**
     * Computes pressure from Van der Waals EOS:
     * P = (R * T) / (V_m - b) - a / V_m^2
     */
    @Override
    public double pressure(double temperature, double volumeMolar) {
        return (R * temperature) / (volumeMolar - b) - a / (volumeMolar * volumeMolar);
    }
}
