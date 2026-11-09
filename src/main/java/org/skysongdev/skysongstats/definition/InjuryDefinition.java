package org.skysongdev.skysongstats.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// An injury from injuries.yml. message is said when the injury is awarded, with
// {character} and {injury} filled in; empty for none. While a character has the
// injury it gets the modifiers, and the conditions (condition id -> level).
public record InjuryDefinition(String id, String name, String description, Icon icon, String severity,
                               String message, List<EffectModifier> modifiers, Map<String, Integer> conditions) {
    public InjuryDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        modifiers = List.copyOf(modifiers);
        conditions = Collections.unmodifiableMap(new LinkedHashMap<>(conditions));
    }
}
