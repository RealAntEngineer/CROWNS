package com.rae.crowns.content.hazards.radiation;

public class Contamination implements IContamination {
    private double contamination = 0;

    @Override
    public double getRads() {
        return contamination;
    }

    @Override
    public void setRads(double contamination) {
        this.contamination = contamination;
    }
}
