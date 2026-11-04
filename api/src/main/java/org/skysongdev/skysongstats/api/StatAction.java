package org.skysongdev.skysongstats.api;

/** What a stat change does, the same actions as the {@code commands:} block of stats.yml. */
public enum StatAction {
    /** Sets the stat's base value, like {@code /stats set}. The amount may be negative. */
    SET,
    /** Adds to the stat, like {@code /heal}. */
    ADD,
    /** Takes off the stat, like {@code /stats reduce}. */
    REDUCE,
    /**
     * Takes off a bar, letting its shield (temp HP for HP) absorb the amount first, like
     * {@code /damage}. Only for bars with a {@code shield}.
     */
    DAMAGE
}
