package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A character's sheet fields as saved, the text {@code /character view} shows. A field
 * nobody set yet holds {@code "[Not Set]"}. A copy: it doesn't change when the sheet does.
 */
public record CharacterSheet(@NotNull CharacterId character, @NotNull String name, @NotNull String age,
                             @NotNull String gender, @NotNull String ancestry, @NotNull String pronouns,
                             @NotNull String description) {
    public CharacterSheet {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(age, "age");
        Objects.requireNonNull(gender, "gender");
        Objects.requireNonNull(ancestry, "ancestry");
        Objects.requireNonNull(pronouns, "pronouns");
        Objects.requireNonNull(description, "description");
    }
}
