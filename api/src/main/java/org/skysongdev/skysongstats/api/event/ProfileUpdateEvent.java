package org.skysongdev.skysongstats.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.Objects;
import java.util.Optional;

/**
 * Fired after a player's active profile changed: today, a {@code /profile switch}. Stats,
 * skills, proficiencies and LuckPerms nodes already follow the new profile when it fires.
 *
 * <p>Fired on the server's main thread. Not cancellable: the change has already happened.
 */
public final class ProfileUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CharacterId previous;
    private final CharacterId character;

    /** Fired by SkysongStats only. */
    @ApiStatus.Internal
    public ProfileUpdateEvent(@NotNull Player player, @Nullable CharacterId previous, @NotNull CharacterId character) {
        this.player = Objects.requireNonNull(player, "player");
        this.previous = previous;
        this.character = Objects.requireNonNull(character, "character");
    }

    /** The player whose profile changed. */
    public @NotNull Player getPlayer() {
        return player;
    }

    /** The character the player was playing before, empty when they had none. */
    public @NotNull Optional<CharacterId> getPrevious() {
        return Optional.ofNullable(previous);
    }

    /** The character the player is playing now. */
    public @NotNull CharacterId getCharacter() {
        return character;
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
