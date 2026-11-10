package org.skysongdev.skysongstats.storage;

import java.util.Objects;

// One adjustment a character's ancestry put on a stat's base value, as it was on
// the day: the stat id, the amount and where it came from (the ancestry's fixed
// stats: or one of its choices), so it can be taken off again exactly.
public record StoredAncestryAdjustment(String stat, int amount, String source) {
    public static final String FIXED = "fixed";
    private static final String CHOICE_PREFIX = "choice:";

    public StoredAncestryAdjustment {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(source, "source");
    }

    // The source of the choice at this position, counted from 1 as in ancestries.yml.
    public static String choice(int position) {
        return CHOICE_PREFIX + position;
    }

    public boolean isFixed() {
        return FIXED.equals(source);
    }
}
