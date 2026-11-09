package org.skysongdev.skysongstats.storage;

// A condition given to a character directly (/condition award or the API), at a
// level of 1 or more, awarded at epoch millis.
public record StoredCondition(int level, long awardedAt) {
    public StoredCondition {
        if (level < 1) {
            throw new IllegalArgumentException("A condition's level is at least 1, got " + level);
        }
    }
}
