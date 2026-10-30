package org.skysongdev.skysongstats.definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// The proficiencies loaded from proficiencies.yml, in file order, with lookups by
// id, alias and skill. Immutable: a reload builds a new one.
public final class ProficiencyDefinitions {
    private final Map<String, ProficiencyDefinition> byId = new LinkedHashMap<>();
    private final Map<String, ProficiencyDefinition> byName = new LinkedHashMap<>();

    // The loader has already checked that ids and aliases are unique.
    ProficiencyDefinitions(Collection<ProficiencyDefinition> proficiencies) {
        for (ProficiencyDefinition proficiency : proficiencies) {
            byId.put(proficiency.id(), proficiency);
            byName.put(proficiency.id(), proficiency);
            proficiency.aliases().forEach(alias -> byName.put(alias, proficiency));
        }
    }

    public List<ProficiencyDefinition> all() {
        return List.copyOf(byId.values());
    }

    public Optional<ProficiencyDefinition> get(String id) {
        return Optional.ofNullable(byId.get(normalize(id)));
    }

    // Finds a proficiency by its id or one of its aliases, ignoring case.
    public Optional<ProficiencyDefinition> find(String idOrAlias) {
        return Optional.ofNullable(byName.get(normalize(idOrAlias)));
    }

    // The proficiencies that belong to a skill, in file order.
    public List<ProficiencyDefinition> ofSkill(String skillId) {
        String skill = normalize(skillId);
        return byId.values().stream().filter(proficiency -> proficiency.skill().equals(skill)).toList();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
