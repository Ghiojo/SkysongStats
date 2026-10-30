package org.skysongdev.skysongstats.definition;

// One tier from proficiencies.yml. extra is what this tier costs on top of the
// tier before it; the first tier and the first paid tier have none.
public record ProficiencyTier(String id, String name, String color, int extra) {
}
