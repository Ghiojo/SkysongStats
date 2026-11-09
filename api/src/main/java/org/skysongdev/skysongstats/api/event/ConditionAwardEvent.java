package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.api.ActiveCondition;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Fired after a condition became current on a character, or changed level: given
 * directly, or because an injury or another condition now gives it. A change of what
 * gives it, at the same level, doesn't fire it.
 *
 * <p>Fired on the server's main thread, after the change. Not cancellable: the change has
 * already happened.
 *
 * @since 1.2.0
 */
public final class ConditionAwardEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;
    private final ActiveCondition condition;
    private final Integer previousLevel;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public ConditionAwardEvent(@NotNull CharacterId character, @NotNull ActiveCondition condition,
                               @Nullable Integer previousLevel, @NotNull ChangeCause cause) {
        this.character = Objects.requireNonNull(character, "character");
        this.condition = Objects.requireNonNull(condition, "condition");
        this.previousLevel = previousLevel;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character it happened to. */
    public @NotNull CharacterId getCharacter() {
        return character;
    }

    /** The condition as the character has it now. */
    public @NotNull ActiveCondition getCondition() {
        return condition;
    }

    /**
     * The level before.
     *
     * @return the level, empty when the character didn't have the condition
     */
    public @NotNull OptionalInt getPreviousLevel() {
        return previousLevel == null ? OptionalInt.empty() : OptionalInt.of(previousLevel);
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
