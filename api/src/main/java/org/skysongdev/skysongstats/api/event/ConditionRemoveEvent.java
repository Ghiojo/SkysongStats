package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;

/**
 * Fired after a condition stopped being current on a character: its direct award was
 * removed, or the injury or condition that gave it went.
 *
 * <p>Fired on the server's main thread, after the change. Not cancellable: the change has
 * already happened.
 *
 * @since 1.2.0
 */
public final class ConditionRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;
    private final String conditionId;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public ConditionRemoveEvent(@NotNull CharacterId character, @NotNull String conditionId,
                                @NotNull ChangeCause cause) {
        this.character = Objects.requireNonNull(character, "character");
        this.conditionId = Objects.requireNonNull(conditionId, "conditionId");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character it happened to. */
    public @NotNull CharacterId getCharacter() {
        return character;
    }

    /** The id of the condition the character no longer has. */
    public @NotNull String getConditionId() {
        return conditionId;
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
