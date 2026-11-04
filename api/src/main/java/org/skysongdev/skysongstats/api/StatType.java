package org.skysongdev.skysongstats.api;

/** The two kinds of stats in stats.yml. */
public enum StatType {
    /** A plain value, such as Strength or AC. */
    VALUE,
    /** A bar with a current value and a max, such as HP. Its max is named {@code max_<id>}. */
    BAR
}
