package org.skysongdev.skysongstats.definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// The conditions loaded from conditions.yml, in file order, found by id ignoring
// case. Immutable: a reload builds a new one.
public final class ConditionDefinitions {
    private final Map<String, ConditionDefinition> byId = new LinkedHashMap<>();

    // The loader has already checked that ids are unique.
    ConditionDefinitions(Collection<ConditionDefinition> conditions) {
        conditions.forEach(condition -> byId.put(condition.id(), condition));
    }

    public List<ConditionDefinition> all() {
        return List.copyOf(byId.values());
    }

    public Optional<ConditionDefinition> get(String id) {
        return Optional.ofNullable(byId.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT)));
    }
}
