package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Which profiles (characters) players have, and which one each player is playing. Read
 * only: profiles are created, switched and deleted by the player with {@code /profile}.
 *
 * <p>Every profile is kept in memory, so these work for offline players too.
 *
 * <p>Only players' profiles: the characters other plugins own (see {@link Characters}) are
 * never listed here, and {@link #exists(CharacterId)} is false for them.
 *
 * <p>Must be called on the server's main thread, and throws {@link IllegalStateException} otherwise.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface Profiles {

    /**
     * The character a player is playing now: their active profile.
     *
     * @param player the player's UUID
     * @return the character, empty when the player has never joined or has no active profile
     */
    @NotNull Optional<CharacterId> active(@NotNull UUID player);

    /**
     * Every character a player has, sorted by profile name ignoring case.
     *
     * @param player the player's UUID
     * @return the characters, an unmodifiable list, empty when the player has none
     */
    @NotNull List<CharacterId> all(@NotNull UUID player);

    /**
     * Whether this character exists: the player has a profile with this exact name.
     *
     * @param character the character
     * @return true when it exists
     */
    boolean exists(@NotNull CharacterId character);

    /**
     * Whether this character has finished {@code /setup}.
     *
     * @param character the character
     * @return true when it finished /setup, false when it hasn't or doesn't exist
     */
    boolean isSetUp(@NotNull CharacterId character);
}
