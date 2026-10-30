package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Optional;

// The tiers from proficiencies.yml, lowest first. Every proficiency starts at the
// first tier for free. The loader has checked there are at least two tiers.
public record ProficiencyTiers(List<ProficiencyTier> all) {
    public ProficiencyTiers {
        all = List.copyOf(all);
    }

    public ProficiencyTier first() {
        return all.getFirst();
    }

    public ProficiencyTier top() {
        return all.getLast();
    }

    public boolean isTop(ProficiencyTier tier) {
        return all.indexOf(tier) == all.size() - 1;
    }

    // The tier after this one, or empty at the top.
    public Optional<ProficiencyTier> next(ProficiencyTier tier) {
        int index = all.indexOf(tier);
        return index < 0 || index == all.size() - 1 ? Optional.empty() : Optional.of(all.get(index + 1));
    }

    // Whether tier is the same as other or above it.
    public boolean atLeast(ProficiencyTier tier, ProficiencyTier other) {
        return all.indexOf(tier) >= all.indexOf(other);
    }

    public Optional<ProficiencyTier> get(String id) {
        return all.stream().filter(tier -> tier.id().equals(id)).findFirst();
    }

    // The points needed to reach this tier from the one before it, for a
    // proficiency whose first paid tier costs firstCost. Each tier after that adds
    // its extra on top of the previous tier's cost: 4, then 4 + 2, then 6 + 2.
    public int cost(ProficiencyTier tier, int firstCost) {
        int index = all.indexOf(tier);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown tier " + tier.id());
        }
        if (index == 0) {
            return 0;
        }
        int cost = firstCost;
        for (int i = 2; i <= index; i++) {
            cost += all.get(i).extra();
        }
        return cost;
    }
}
