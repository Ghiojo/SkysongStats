package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One proficiency's tier before and after a change, up or down.
 *
 * @param character   the character
 * @param proficiency the proficiency
 * @param before      the tier before
 * @param after       the tier after
 * @param tierUp      whether it went up
 */
public record TierChange(@NotNull CharacterId character, @NotNull Proficiency proficiency, @NotNull Tier before,
                         @NotNull Tier after, boolean tierUp) {

    /**
     * @throws NullPointerException when any part is null
     */
    public TierChange {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(proficiency, "proficiency");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }

    /** Whether the tier is different afterwards. */
    public boolean changed() {
        return !before.id().equals(after.id());
    }
}
