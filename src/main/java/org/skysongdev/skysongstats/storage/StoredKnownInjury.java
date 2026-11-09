package org.skysongdev.skysongstats.storage;

import java.util.Objects;

// An injury a character knows: when it learnt it (epoch millis) and how.
public record StoredKnownInjury(long learntAt, LearnSource learntBy) {
    public StoredKnownInjury {
        Objects.requireNonNull(learntBy, "learntBy");
    }
}
