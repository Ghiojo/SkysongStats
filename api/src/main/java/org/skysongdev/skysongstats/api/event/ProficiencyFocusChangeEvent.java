package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Focus;

import java.util.Objects;
import java.util.Optional;

/**
 * Fired after a character's focus changed: a new focus, points added, removed or set, or
 * the focus cleared when its proficiency reached the top tier.
 *
 * <p>Fired on the server's main thread. Not cancellable: the change has already happened.
 */
public final class ProficiencyFocusChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;
    private final Focus before;
    private final Focus after;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public ProficiencyFocusChangeEvent(@NotNull CharacterId character, @Nullable Focus before, @Nullable Focus after,
                                       @NotNull ChangeCause cause) {
        this.character = Objects.requireNonNull(character, "character");
        this.before = before;
        this.after = after;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character whose focus changed. */
    public @NotNull CharacterId getCharacter() {
        return character;
    }

    /** The focus before the change, empty when there was none. */
    public @NotNull Optional<Focus> getBefore() {
        return Optional.ofNullable(before);
    }

    /** The focus after the change, empty when there is none now. */
    public @NotNull Optional<Focus> getAfter() {
        return Optional.ofNullable(after);
    }

    /** What changed the focus. */
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
