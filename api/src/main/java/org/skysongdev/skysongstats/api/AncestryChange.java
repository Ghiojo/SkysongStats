package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * A change to a character's ancestry: what it had before and has after.
 *
 * @since 1.4.0
 */
public final class AncestryChange {
    private final CharacterId character;
    private final CharacterAncestry before;
    private final CharacterAncestry after;

    /**
     * @param before its ancestry before; null when it had none
     * @param after  its ancestry after; null when it has none now
     */
    public AncestryChange(@NotNull CharacterId character, @Nullable CharacterAncestry before,
                          @Nullable CharacterAncestry after) {
        this.character = Objects.requireNonNull(character, "character");
        this.before = before;
        this.after = after;
    }

    /** The character it happened to. */
    public @NotNull CharacterId character() {
        return character;
    }

    /** Its ancestry before; empty when it had none. */
    public @NotNull Optional<CharacterAncestry> before() {
        return Optional.ofNullable(before);
    }

    /** Its ancestry now; empty when it has none (after a remove). */
    public @NotNull Optional<CharacterAncestry> after() {
        return Optional.ofNullable(after);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AncestryChange change && character.equals(change.character)
                && Objects.equals(before, change.before) && Objects.equals(after, change.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(character, before, after);
    }

    @Override
    public String toString() {
        return "AncestryChange{character=" + character + ", before=" + before + ", after=" + after + "}";
    }
}
