package com.rae.crowns.content.hazards;

import com.rae.crowns.content.hazards.radiation.ItemRadiation;

public class HazardData {
    boolean doesOverride = false;
    /*
     * MUTEX, even more precise to make only specific entries mutually exclusive
     * Does the opposite of overrides, if a previous entry collides with this one, this one will yield.
     *
     * RESERVED BITS (please keep this up to date)
     * -1: oredict ("ingotX")
     * Ughh
     */
    int mutexBits = 0b0000_0000_0000_0000_0000_0000_0000_0000;

    HazardEntry entry;

    public HazardData addEntry(ItemRadiation.DecayContainer entry) {
        return this.addEntry(entry, false);
    }

    public HazardData addEntry(ItemRadiation.DecayContainer hazard, boolean override) {
        this.entry = new HazardEntry(hazard);
        this.doesOverride = override;
        return this;
    }

    public HazardData setMutex(int mutex) {
        this.mutexBits = mutex;
        return this;
    }

    public int getMutex() {
        return mutexBits;
    }
}

// Github desktop is retarded