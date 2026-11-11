package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A + or − to one stat's base value, as ancestries.yml gives it.
 *
 * @param stat   the stat's id in stats.yml
 * @param amount how much it adds (positive) or takes (negative); never 0
 * @since 1.4.0
 */
public record AncestryAdjustment(@NotNull String stat, int amount) {

    /**
     * @throws NullPointerException when the stat is null
     */
    public AncestryAdjustment {
        Objects.requireNonNull(stat, "stat");
    }
}
