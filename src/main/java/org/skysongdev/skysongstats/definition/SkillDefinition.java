package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Optional;

// One skill from skills.yml. shortName is what the /skills book shows, and setup
// is null for skills that can't be picked in /setup.
public record SkillDefinition(
        String id,
        String name,
        String shortName,
        String color,
        List<String> aliases,
        int baseline,
        SkillSetup setup) {

    public SkillDefinition {
        aliases = List.copyOf(aliases);
    }

    public Optional<SkillSetup> setupOptions() {
        return Optional.ofNullable(setup);
    }
}
