package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One stat modifier from the {@code effects: modifiers:} of an injury or condition, as
 * written in injuries.yml or conditions.yml.
 *
 * @param target   the stat id, or {@code max_<bar>} for a bar's max (aliases already
 *                 resolved)
 * @param amount   what it adds; negative takes off. For a per-level modifier, +1 or -1
 * @param perLevel whether it was written with {@code $}: it then counts once per level of
 *                 the condition ({@code con-$} at level 3 takes off 3)
 * @since 1.2.0
 */
public record EffectModifier(@NotNull String target, int amount, boolean perLevel) {

    /**
     * @throws NullPointerException when the target is null
     */
    public EffectModifier {
        Objects.requireNonNull(target, "target");
    }
}
