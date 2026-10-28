package org.skysongdev.skysongstats.service.downtime;

import java.util.UUID;

/**
 * One change to a downtime pool, as returned by {@link DowntimeService} and
 * passed to its {@link DowntimeChangeListener}.
 *
 * @param profile the profile for {@link DowntimePool#CHARACTER}, {@code null} for {@link DowntimePool#GLOBAL}
 */
public record DowntimeChange(UUID player, DowntimePool pool, String profile, int oldValue, int newValue, Cause cause) {

    public enum Cause {
        ADD,
        REMOVE,
        RESET
    }

    /** How much the pool actually moved, after clamping. Negative for removals. */
    public int delta() {
        return newValue - oldValue;
    }
}
