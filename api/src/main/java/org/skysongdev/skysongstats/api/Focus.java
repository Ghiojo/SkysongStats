package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The proficiency a character is training and its points towards the next tier.
 *
 * @param proficiency the focused proficiency
 * @param points      the points it has towards its next tier
 */
public record Focus(@NotNull Proficiency proficiency, int points) {

    /**
     * @throws NullPointerException when the proficiency is null
     */
    public Focus {
        Objects.requireNonNull(proficiency, "proficiency");
    }
}
