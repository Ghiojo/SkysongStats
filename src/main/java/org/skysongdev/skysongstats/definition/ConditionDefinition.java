package org.skysongdev.skysongstats.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// A condition from conditions.yml. description may hold $, the level a character
// has the condition at. While a character has it, it gets the modifiers and the
// conditions (condition id -> level); both are empty for a condition with no effect.
public record ConditionDefinition(String id, String name, String description, Icon icon,
                                  List<EffectModifier> modifiers, Map<String, Integer> conditions) {
    public ConditionDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(icon, "icon");
        modifiers = List.copyOf(modifiers);
        conditions = Collections.unmodifiableMap(new LinkedHashMap<>(conditions));
    }
}
