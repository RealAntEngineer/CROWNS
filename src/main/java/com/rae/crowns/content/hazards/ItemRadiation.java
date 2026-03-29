package com.rae.crowns.content.hazards;

public class ItemRadiation {
    public static class DecayContainer {
        public double specific_activity; // Measured in Bq

        // Decay channel PROBABILITY! Must be <1
        public double alpha;
        public double beta_plus;
        public double beta_minus;
        public double sf;

        public double daughter_excited; // measured in MeV, divide accordingly
        public double branching_ratio; // These two are used to calculate a basic estimate of gammas

        public double alpha_energy; // Also in MeV
        public double beta_energy; // Even more bs

        public DecayContainer(double specific_activity, double alpha, double alpha_energy, double beta_minus, double beta_energy, double beta_plus, double sf, double daughter_excited, double branching_ratio) {
            this.specific_activity = specific_activity;
            this.alpha = alpha;
            this.alpha_energy = alpha_energy;
            this.beta_plus = beta_plus;
            this.beta_minus = beta_minus;
            this.beta_energy = beta_energy;
            this.sf = sf;
            this.daughter_excited = daughter_excited;
            this.branching_ratio = branching_ratio;
        }

        public DecayContainer(double specific_activity, double alpha, double alpha_energy, double daughter_excited, double branching_ratio) {
            this(specific_activity, alpha, alpha_energy, 0D, 0D, 0D, 0D, daughter_excited, branching_ratio);
        }

        public DecayContainer multiply(double v) {
            DecayContainer container = this.copy();
            container.specific_activity *= v;
            return container;
        }

        public DecayContainer add(double v) {
            DecayContainer container = this.copy();
            container.specific_activity += v;
            return container;
        }

        public DecayContainer copy() {
            return new DecayContainer(specific_activity, alpha, alpha_energy, beta_minus, beta_energy, beta_plus, sf, daughter_excited, branching_ratio);
        }

        public double getGammas() {
            return specific_activity * branching_ratio;
        }

        public double getEnergyFluence(double distance) {
            return (getGammas() * daughter_excited) / (4*Math.PI*Math.pow(distance, 2));
        }

        public double getRoentgen(double distance) { // in R/s
            final double airMassAbsorptionCoefficient = 0.029D;

            return getEnergyFluence(distance) * airMassAbsorptionCoefficient * (1.828e-11); // I forgot what this constant does
        } // 3.6 roentgens. Not great, not terrible
    }

    /**
     * Use for special radiation, this is currently only used by fission
     */
    public static class SpecialContainer {
        public double intensity; // Gammas per event

        public double gamma_energy;

        public SpecialContainer(double intensity, double gamma_energy) {
            this.intensity = intensity;
            this.gamma_energy = gamma_energy;
        }

        public double getEnergyFluence(double distance) {
            return (intensity * gamma_energy) / (4*Math.PI*Math.pow(distance, 2));
        }

        public double getRoentgen(double distance) { // in R/s
            final double airMassAbsorptionCoefficient = 0.029D;

            return getEnergyFluence(distance) * airMassAbsorptionCoefficient * (1.828e-11); // I forgot what this constant does
        }
    }
}
