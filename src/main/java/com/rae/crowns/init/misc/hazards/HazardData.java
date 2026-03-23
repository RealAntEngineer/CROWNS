package com.rae.crowns.init.misc.hazards;

import com.rae.crowns.init.misc.hazards.types.HazardTypeBase;

import java.util.ArrayList;
import java.util.List;

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

    List<HazardEntry> entries = new ArrayList<>();

    public HazardData addEntry(HazardTypeBase entry) {
        return this.addEntry(entry, 1F);
    }

    public HazardData addEntry(HazardTypeBase entry, double level) {
        return this.addEntry(entry, level, false);
    }

    public HazardData addEntry(HazardTypeBase hazard, double level, boolean override) {
        this.entries.add(new HazardEntry(hazard, level));
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