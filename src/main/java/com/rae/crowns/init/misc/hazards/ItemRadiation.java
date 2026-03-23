package com.rae.crowns.init.misc.hazards;

public class ItemRadiation {
    public static class DecayContainer {
        public double specific_activity; // Measured in Bq

        // Decay channel PROBABILITY! Must be <1 and must sum to 1
        public double alpha;
        public double beta_plus;
        public double beta_minus;
        public double sf;
        // Will implement de-excitation gamma rays soon

        public DecayContainer(double specific_activity, double alpha, double beta_minus, double beta_plus, double sf) {
            this.specific_activity = specific_activity;
            this.alpha = alpha;
            this.beta_plus = beta_plus;
            this.beta_minus = beta_minus;
            this.sf = sf;
        }

        public DecayContainer(double specific_activity, double alpha) {
            this(specific_activity, alpha, 0F, 0F, 0F);
        }

        public DecayContainer multiply(double v) {
            this.specific_activity *= v;
            return this;
        }

        public DecayContainer copy() {
            return new DecayContainer(specific_activity, alpha, beta_minus, beta_minus, sf);
        }


    }
}
