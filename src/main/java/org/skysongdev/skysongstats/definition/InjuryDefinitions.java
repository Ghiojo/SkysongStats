package org.skysongdev.skysongstats.definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// The severities and injuries loaded from injuries.yml, in file order, found by id
// ignoring case. Immutable: a reload builds a new one.
public final class InjuryDefinitions {
    private final List<String> severities;
    private final Map<String, InjuryDefinition> byId = new LinkedHashMap<>();

    // The loader has already checked that ids are unique and every injury's
    // severity is in severities.
    InjuryDefinitions(List<String> severities, Collection<InjuryDefinition> injuries) {
        this.severities = List.copyOf(severities);
        injuries.forEach(injury -> byId.put(injury.id(), injury));
    }

    public List<InjuryDefinition> all() {
        return List.copyOf(byId.values());
    }

    public Optional<InjuryDefinition> get(String id) {
        return Optional.ofNullable(byId.get(normalize(id)));
    }

    // Every severity injuries.yml allows, in its order.
    public List<String> severities() {
        return severities;
    }

    // The severities at least one injury has, in the order of severities(). Not
    // every severity needs injuries.
    public List<String> usedSeverities() {
        return severities.stream()
                .filter(severity -> byId.values().stream().anyMatch(injury -> injury.severity().equals(severity)))
                .toList();
    }

    // The injuries of one severity, in file order.
    public List<InjuryDefinition> ofSeverity(String severity) {
        String wanted = normalize(severity);
        return byId.values().stream().filter(injury -> injury.severity().equals(wanted)).toList();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
