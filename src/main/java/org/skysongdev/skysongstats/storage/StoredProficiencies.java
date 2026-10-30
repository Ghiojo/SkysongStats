package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Optional;

// What is saved for one character's proficiencies: the tier id of each
// proficiency, and its focus when it has one (null otherwise).
public record StoredProficiencies(Map<String, String> tiers, StoredFocus focus) {
    public StoredProficiencies {
        tiers = Map.copyOf(tiers);
    }

    public Optional<StoredFocus> focusOptions() {
        return Optional.ofNullable(focus);
    }
}
