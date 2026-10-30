package org.skysongdev.skysongstats.definition;

// Everything proficiencies.yml defines: the tiers and the proficiencies.
public record ProficienciesConfig(ProficiencyTiers tiers, ProficiencyDefinitions proficiencies) {

    // The points a proficiency needs to reach this tier from the one before it.
    public int cost(ProficiencyDefinition proficiency, ProficiencyTier tier) {
        return tiers.cost(tier, proficiency.cost());
    }
}
