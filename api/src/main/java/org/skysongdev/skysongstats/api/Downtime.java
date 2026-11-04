package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Global and character downtime, the same pools {@code /downtime} shows and changes.
 *
 * <p>Only players and characters SkysongStats knows answer: a player with at least one
 * profile, or a character that exists. Must be called on the server's main thread, and
 * throws {@link IllegalStateException} otherwise.
 *
 * <p>Writes work like {@code /downtime}: adding stops at the pool's max, removing stops at
 * 0, the change is saved straight away and the downtime event fires. They don't check
 * permissions: a plugin acting for a player checks that player's permission itself.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface Downtime {

    /** The current limits from config.yml. Safe from any thread. */
    @NotNull DowntimeLimits limits();

    /**
     * The downtime in a pool.
     *
     * @return the downtime, empty when the player has no profiles or the character doesn't exist
     */
    @NotNull OptionalInt get(@NotNull DowntimeTarget target);

    /**
     * Whether the player spends from their global downtime by default; otherwise from the
     * character they are playing.
     *
     * @return the preference, empty when the player has no profiles
     */
    @NotNull Optional<Boolean> spendsGlobal(@NotNull UUID player);

    /**
     * Sets the player's spending preference, like {@code /downtime preference}.
     *
     * @return whether it was set; false when the player has no profiles
     */
    boolean setSpendsGlobal(@NotNull UUID player, boolean spendGlobal);

    /**
     * Adds downtime, like {@code /downtime add}, stopping at the pool's max.
     *
     * @param amount 0 or more
     * @return the change, empty when the player has no profiles or the character doesn't exist
     * @throws IllegalArgumentException when the amount is negative
     */
    @NotNull Optional<DowntimeChange> add(@NotNull DowntimeTarget target, int amount);

    /**
     * Removes downtime, like {@code /downtime remove}, stopping at 0.
     *
     * @param amount 0 or more
     * @return the change, empty when the player has no profiles or the character doesn't exist
     * @throws IllegalArgumentException when the amount is negative
     */
    @NotNull Optional<DowntimeChange> remove(@NotNull DowntimeTarget target, int amount);

    /**
     * Spends downtime from the pool the player prefers: global, or the character they are
     * playing. Like {@code /downtime remove} without a pool.
     *
     * @param amount 0 or more
     * @return the change, empty when the player has no profiles, or prefers character
     *         downtime and has no active profile
     * @throws IllegalArgumentException when the amount is negative
     */
    @NotNull Optional<DowntimeChange> spend(@NotNull UUID player, int amount);
}
