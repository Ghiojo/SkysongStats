package org.skysongdev.skysongstats.storage;

import java.util.List;
import java.util.Map;

// What is saved for one character: base values by stat id (a bar's current value)
// and modifiers in the order they were added. Stats without a saved value are at
// their baseline.
public record StoredStats(Map<String, Integer> values, List<StoredModifier> modifiers) {
    public StoredStats {
        values = Map.copyOf(values);
        modifiers = List.copyOf(modifiers);
    }
}
