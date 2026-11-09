package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;

/**
 * Fired after a character learnt an injury: by getting it, by {@code /injury discover}, or
 * through the API. Not fired by {@code /ssimport}.
 *
 * <p>Fired on the server's main thread, after the change. Not cancellable: the change has
 * already happened.
 *
 * @since 1.2.0
 */
public final class InjuryDiscoverEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;
    private final String injuryId;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public InjuryDiscoverEvent(@NotNull CharacterId character, @NotNull String injuryId, @NotNull ChangeCause cause) {
        this.character = Objects.requireNonNull(character, "character");
        this.injuryId = Objects.requireNonNull(injuryId, "injuryId");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character it happened to. */
    public @NotNull CharacterId getCharacter() {
        return character;
    }

    /** The id of the injury the character now knows. */
    public @NotNull String getInjuryId() {
        return injuryId;
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
