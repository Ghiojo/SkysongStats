package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;

/**
 * An injury a character has now.
 *
 * @param typeId    the injury's id in injuries.yml; {@link Injuries#type(String)} gives the rest
 * @param awardedAt when it was given, or given again
 * @since 1.2.0
 */
public record Injury(@NotNull String typeId, @NotNull Instant awardedAt) {

    /**
     * @throws NullPointerException when any part is null
     */
    public Injury {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(awardedAt, "awardedAt");
    }
}
