package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One downtime pool before and after a change.
 *
 * @param target the pool that changed
 * @param kind   what kind of change it was
 * @param before the downtime before
 * @param after  the downtime after, once clamped to 0 and the pool's max
 */
public record DowntimeChange(@NotNull DowntimeTarget target, @NotNull Kind kind, int before, int after) {

    /** What kind of change it was, as the downtime commands name them. */
    public enum Kind {
        /** Downtime added, like {@code /downtime add}. */
        ADD,
        /** Downtime removed or spent, like {@code /downtime remove}. */
        REMOVE,
        /** A new downtime period, {@code /downtime resetall}. */
        RESET
    }

    /**
     * @throws NullPointerException when the target or the kind is null
     */
    public DowntimeChange {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(kind, "kind");
    }

    /** How much the pool actually moved, after clamping. Negative for removals. */
    public int delta() {
        return after - before;
    }

    /** Whether the downtime is different afterwards. */
    public boolean changed() {
        return before != after;
    }
}
