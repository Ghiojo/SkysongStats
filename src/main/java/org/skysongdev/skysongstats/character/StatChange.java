package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

// A stat's total before and after a change, for messages and events. For a bar
// the total is its current value.
public record StatChange(ProfileKey character, StatDefinition stat, int oldValue, int newValue) {
    public boolean changed() {
        return oldValue != newValue;
    }
}
