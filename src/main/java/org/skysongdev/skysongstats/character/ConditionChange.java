package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Objects;
import java.util.Optional;

// A condition that started, stopped, or changed level or sources on a character.
// before is empty when it started, after is empty when it stopped.
public record ConditionChange(ProfileKey character, Optional<ActiveCondition> before, Optional<ActiveCondition> after) {
    public ConditionChange {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (before.isEmpty() && after.isEmpty()) {
            throw new IllegalArgumentException("A condition change needs a before or an after");
        }
    }

    public boolean started() {
        return before.isEmpty();
    }

    public boolean stopped() {
        return after.isEmpty();
    }

    // Started, stopped, or its level went up or down; a change of sources alone isn't one.
    public boolean levelChanged() {
        return !before.map(ActiveCondition::level).equals(after.map(ActiveCondition::level));
    }
}
