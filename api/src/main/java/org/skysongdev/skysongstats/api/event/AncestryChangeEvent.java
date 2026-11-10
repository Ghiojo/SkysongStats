package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.api.CharacterAncestry;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;
import java.util.Optional;

/**
 * Fired after a character's ancestry changed: picked in /setup, {@code /ancestry set},
 * {@code choose}, {@code rechoose} or {@code remove}, or the API. The stat changes it made
 * fire their own {@link StatChangeEvent}s.
 *
 * <p>Fired on the server's main thread, after the change. Not cancellable: the change has
 * already happened.
 *
 * @since 1.4.0
 */
public final class AncestryChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;
    private final CharacterAncestry before;
    private final CharacterAncestry after;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public AncestryChangeEvent(@NotNull CharacterId character, @Nullable CharacterAncestry before,
                               @Nullable CharacterAncestry after, @NotNull ChangeCause cause) {
        this.character = Objects.requireNonNull(character, "character");
        this.before = before;
        this.after = after;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character it happened to. */
    public @NotNull CharacterId getCharacter() {
        return character;
    }

    /** Its ancestry before; empty when it had none. */
    public @NotNull Optional<CharacterAncestry> getBefore() {
        return Optional.ofNullable(before);
    }

    /** Its ancestry now; empty when it was removed. */
    public @NotNull Optional<CharacterAncestry> getAfter() {
        return Optional.ofNullable(after);
    }

    /** What made it happen. */
    public @NotNull ChangeCause getCause() {
        return cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /** The handlers of this event, as every Bukkit event needs. */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
