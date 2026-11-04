package org.skysongdev.skysongstats.api;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A character's numbers for one stat, the same ones the placeholders show. For a bar, the
 * values without "max" are its current value.
 *
 * <p>Two values are equal when every number is equal.
 */
public final class StatValues {
    private final int base;
    private final int modifiers;
    private final int items;
    private final Integer max;
    private final Integer maxBase;

    /**
     * @param base      the saved value, before modifiers and items
     * @param modifiers the sum of the modifiers on the stat
     * @param items     what Skysong items in the inventory add
     * @param max       a bar's max with its modifiers, null for plain values
     * @param maxBase   a bar's max before its modifiers (the max formula alone), null for plain values
     */
    public StatValues(int base, int modifiers, int items, Integer max, Integer maxBase) {
        this.base = base;
        this.modifiers = modifiers;
        this.items = items;
        this.max = max;
        this.maxBase = maxBase;
    }

    /** The saved value, before modifiers and items: {@code %skysongstats_<id>_base%}. */
    public int base() {
        return base;
    }

    /** The sum of the modifiers on the stat: {@code %skysongstats_<id>_modifiers%}. */
    public int modifiers() {
        return modifiers;
    }

    /** What Skysong items in the player's inventory add to the stat. */
    public int items() {
        return items;
    }

    /** The value with modifiers and items, {@code base + modifiers + items}: {@code %skysongstats_<id>%}. */
    public int total() {
        return base + modifiers + items;
    }

    /** A bar's max with its modifiers: {@code %skysongstats_max_<id>%}. Empty for plain values. */
    public OptionalInt max() {
        return max == null ? OptionalInt.empty() : OptionalInt.of(max);
    }

    /** A bar's max before its modifiers: {@code %skysongstats_max_<id>_base%}. Empty for plain values. */
    public OptionalInt maxBase() {
        return maxBase == null ? OptionalInt.empty() : OptionalInt.of(maxBase);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StatValues values)) {
            return false;
        }
        return base == values.base && modifiers == values.modifiers && items == values.items
                && Objects.equals(max, values.max) && Objects.equals(maxBase, values.maxBase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, modifiers, items, max, maxBase);
    }

    @Override
    public String toString() {
        return "StatValues[total=" + total() + ", base=" + base + ", modifiers=" + modifiers + ", items=" + items
                + (max == null ? "" : ", max=" + max + ", maxBase=" + maxBase) + "]";
    }
}
