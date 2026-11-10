package org.skysongdev.skysongstats.definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// The ancestries loaded from ancestries.yml, in file order, found by id ignoring
// case. Immutable: a reload builds a new one.
public final class AncestryDefinitions {
    private final Map<String, AncestryDefinition> byId = new LinkedHashMap<>();

    // The loader has already checked that ids and slots are unique.
    AncestryDefinitions(Collection<AncestryDefinition> ancestries) {
        ancestries.forEach(ancestry -> byId.put(ancestry.id(), ancestry));
    }

    public static AncestryDefinitions none() {
        return new AncestryDefinitions(List.of());
    }

    public List<AncestryDefinition> all() {
        return List.copyOf(byId.values());
    }

    public Optional<AncestryDefinition> get(String id) {
        return Optional.ofNullable(byId.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT)));
    }

    // With none, /setup skips the ancestry screens.
    public boolean isEmpty() {
        return byId.isEmpty();
    }
}
