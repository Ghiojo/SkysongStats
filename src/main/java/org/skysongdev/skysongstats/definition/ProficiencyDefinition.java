package org.skysongdev.skysongstats.definition;

import java.util.List;

// One proficiency from proficiencies.yml. skill is the id of the skills.yml skill
// it belongs to, and cost is the points its first paid tier needs (its own cost,
// or the file's global one).
public record ProficiencyDefinition(
        String id,
        String name,
        String color,
        String skill,
        List<String> aliases,
        int cost) {

    public ProficiencyDefinition {
        aliases = List.copyOf(aliases);
    }
}
