package org.skysongdev.skysongstats.proficiency;

import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.ProficiencyTiers;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// The LuckPerms nodes of a proficiency's tiers:
// skysongstats.proficiency.<proficiency>.<tier>. Every tier except the first has
// one, and a character has the node of its tier and of every tier below it.
public final class ProficiencyPermissions {
    public static final String PREFIX = "skysongstats.proficiency.";

    private ProficiencyPermissions() {
    }

    public static String node(ProficiencyDefinition proficiency, ProficiencyTier tier) {
        return PREFIX + proficiency.id() + "." + tier.id();
    }

    // Every node the proficiency's tiers give, lowest tier first.
    public static Set<String> all(ProficiencyDefinition proficiency, ProficiencyTiers tiers) {
        return nodes(proficiency, tiers.all());
    }

    // The nodes a character at this tier of the proficiency has.
    public static Set<String> reached(ProficiencyDefinition proficiency, ProficiencyTiers tiers, ProficiencyTier tier) {
        List<ProficiencyTier> all = tiers.all();
        return nodes(proficiency, all.subList(0, all.indexOf(tier) + 1));
    }

    private static Set<String> nodes(ProficiencyDefinition proficiency, List<ProficiencyTier> tiers) {
        Set<String> nodes = new LinkedHashSet<>();
        tiers.stream().skip(1).forEach(tier -> nodes.add(node(proficiency, tier)));
        return nodes;
    }
}
