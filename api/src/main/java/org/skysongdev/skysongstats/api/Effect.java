package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A stat modifier a character has because of a current injury or condition. Effects are
 * worked out from what the character has, never saved, and are not
 * {@link Modifiers saved modifiers}: they go away with the injury or condition.
 *
 * @param target   the stat id, or {@code max_<bar>} for a bar's max
 * @param amount   what it adds now, a condition's per-level modifiers already multiplied by
 *                 its level
 * @param source   whether an injury or a condition gives it
 * @param sourceId the id of that injury or condition
 * @since 1.2.0
 */
public record Effect(@NotNull String target, int amount, @NotNull EffectSource source, @NotNull String sourceId) {

    /**
     * @throws NullPointerException when any part is null
     */
    public Effect {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sourceId, "sourceId");
    }
}
