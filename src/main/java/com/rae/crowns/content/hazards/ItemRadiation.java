package com.rae.crowns.content.hazards;

public class ItemRadiation {
    public static class DecayContainer {
        public double specific_activity; // Measured in Bq

        // Decay channel PROBABILITY! Must be <1 and must sum to 1
        public double alpha;
        public double beta_plus;
        public double beta_minus;
        public double sf;

        public double daughter_excited; // measured in MeV, divide accordingly
        public double branching_ratio; // These two are used to calculate a basic estimate of gammas

        public DecayContainer(double specific_activity, double alpha, double beta_minus, double beta_plus, double sf, double daughter_excited, double branching_ratio) {
            this.specific_activity = specific_activity;
            this.alpha = alpha;
            this.beta_plus = beta_plus;
            this.beta_minus = beta_minus;
            this.sf = sf;
            this.daughter_excited = daughter_excited;
            this.branching_ratio = branching_ratio;
        }

        public DecayContainer(double specific_activity, double alpha, double daughter_excited, double branching_ratio) {
            this(specific_activity, alpha, 0F, 0F, 0F, daughter_excited, daughter_excited);
        }

        public DecayContainer multiply(double v) {
            this.specific_activity *= v;
            return this;
        }

        public DecayContainer copy() {
            return new DecayContainer(specific_activity, alpha, beta_minus, beta_minus, sf, daughter_excited, branching_ratio);
        }

        public double getGammas() {
            return specific_activity * branching_ratio;
        }

        public double getEnergyFluence(double distance) {
            return (getGammas() * daughter_excited) / 4*Math.PI*(Math.pow(distance, 2));
        }

        public double getReontgen(double distance) { // in R/s
            final double airMassAbsorptionCoefficient = 0.029D;

            return getEnergyFluence(distance) * airMassAbsorptionCoefficient * (1.828e-11); // Random ass constant
        } // 3.6 reontgens. Not great, not terrible
    }
}
