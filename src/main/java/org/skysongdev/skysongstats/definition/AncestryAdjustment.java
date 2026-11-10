package org.skysongdev.skysongstats.definition;

import java.util.Objects;

// One + or - an ancestry gives a /setup stat: the stat's id (never an alias) and
// the amount added to its base value; negative takes off, never 0.
public record AncestryAdjustment(String stat, int amount) {
    public AncestryAdjustment {
        Objects.requireNonNull(stat, "stat");
        if (amount == 0) {
            throw new IllegalArgumentException("An ancestry adjustment can't be 0");
        }
    }

    public boolean isBoost() {
        return amount > 0;
    }
}
