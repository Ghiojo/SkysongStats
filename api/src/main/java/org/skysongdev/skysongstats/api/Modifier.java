package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A saved modifier on a character, the same as {@code /modifier add} makes. It adds its
 * amount to a stat's total, or to a bar's max when its target is {@code max_<bar>}.
 *
 * @param id     its permanent id, the one {@link Modifiers#remove(CharacterId, String)} takes
 * @param target the stat id, or {@code max_<bar>} for a bar's max
 * @param amount what it adds; negative takes off
 */
public record Modifier(@NotNull String id, @NotNull String target, int amount) {

    /**
     * @throws NullPointerException when the id or the target is null
     */
    public Modifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(target, "target");
    }
}
