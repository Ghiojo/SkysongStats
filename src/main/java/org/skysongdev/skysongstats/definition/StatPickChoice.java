package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Objects;

// "amount to picks different stats out of from". from holds stat ids in stats.yml
// order; anyStat says the file wrote "from: any" (every /setup stat), so texts can
// say "any stat" instead of listing them.
public record StatPickChoice(String name, int amount, int picks, List<String> from, boolean anyStat,
                             boolean stack) implements AncestryChoice {
    public StatPickChoice {
        Objects.requireNonNull(name, "name");
        from = List.copyOf(from);
        if (amount == 0) {
            throw new IllegalArgumentException("A choice's amount can't be 0");
        }
        if (picks < 1 || picks > from.size()) {
            throw new IllegalArgumentException("A choice must pick between 1 and " + from.size() + " stats");
        }
    }

    public boolean isBoost() {
        return amount > 0;
    }
}
