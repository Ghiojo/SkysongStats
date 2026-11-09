package org.skysongdev.skysongstats.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Injury;

import java.util.Objects;
import java.util.Optional;

/**
 * Fired after a character was given an injury: {@code /injury award} or the API. Also
 * fired when it already had the injury and got it again, with {@link #isNew()} false.
 *
 * <p>A plugin that {@linkplain org.skysongdev.skysongstats.api.Injuries#announceAwards
 * announces awards} says {@link #getMessage()} from here.
 *
 * <p>Fired on the server's main thread, after the change. Not cancellable: the change has
 * already happened.
 *
 * @since 1.2.0
 */
public final class InjuryAwardEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CharacterId character;
    private final Injury injury;
    private final boolean isNew;
    private final String message;
    private final ChangeCause cause;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public InjuryAwardEvent(@NotNull CharacterId character, @NotNull Injury injury, boolean isNew,
                            @Nullable String message, @NotNull ChangeCause cause) {
        this.character = Objects.requireNonNull(character, "character");
        this.injury = Objects.requireNonNull(injury, "injury");
        this.isNew = isNew;
        this.message = message;
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /** The character it happened to. */
    public @NotNull CharacterId getCharacter() {
        return character;
    }

    /** The injury, as the character has it now. */
    public @NotNull Injury getInjury() {
        return injury;
    }

    /** Whether the character didn't have the injury before. */
    public boolean isNew() {
        return isNew;
    }

    /**
     * The injury's award message, with {@code {character}} and {@code {injury}} already
     * filled in, as MiniMessage.
     *
     * @return the message, empty when the injury has none
     */
    public @NotNull Optional<String> getMessage() {
        return Optional.ofNullable(message);
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
