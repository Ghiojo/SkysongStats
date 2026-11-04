package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One stat's total before and after a change. For a bar the total is its current value.
 *
 * @param character the character whose stat changed
 * @param stat      the stat
 * @param before    the total before the change
 * @param after     the total after the change
 */
public record StatChange(@NotNull CharacterId character, @NotNull Stat stat, int before, int after) {

    /**
     * @throws NullPointerException when the character or the stat is null
     */
    public StatChange {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(stat, "stat");
    }

    /** Whether the total is different afterwards. */
    public boolean changed() {
        return before != after;
    }
}
