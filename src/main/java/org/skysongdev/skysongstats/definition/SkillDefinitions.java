package org.skysongdev.skysongstats.definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// The skills loaded from skills.yml, in file order, with lookups by id and alias.
// Immutable: a reload builds a new one.
public final class SkillDefinitions {
    private final Map<String, SkillDefinition> byId = new LinkedHashMap<>();
    private final Map<String, SkillDefinition> byName = new LinkedHashMap<>();

    // The loader has already checked that ids and aliases are unique.
    SkillDefinitions(Collection<SkillDefinition> skills) {
        for (SkillDefinition skill : skills) {
            byId.put(skill.id(), skill);
            byName.put(skill.id(), skill);
            skill.aliases().forEach(alias -> byName.put(alias, skill));
        }
    }

    public List<SkillDefinition> all() {
        return List.copyOf(byId.values());
    }

    public Optional<SkillDefinition> get(String id) {
        return Optional.ofNullable(byId.get(normalize(id)));
    }

    // Finds a skill by its id or one of its aliases, ignoring case ("AH", "T&E").
    public Optional<SkillDefinition> find(String idOrAlias) {
        return Optional.ofNullable(byName.get(normalize(idOrAlias)));
    }

    public List<SkillDefinition> setupSkills() {
        return byId.values().stream().filter(skill -> skill.setup() != null).toList();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
