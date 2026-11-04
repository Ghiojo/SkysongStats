package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The stats of stats.yml and each character's values, bars included.
 *
 * <p>A stat is found by its id or one of its aliases from stats.yml, ignoring case, the
 * same names the stat commands take. Nothing else matches.
 *
 * <p>{@link #all()} and {@link #find(String)} are safe from any thread. The methods that
 * read or change a character must be called on the server's main thread, and throw
 * {@link IllegalStateException} otherwise.
 *
 * <p>Writes work like the stat commands: the same limits (a stat's minimum, a capped bar's
 * max), saved straight away, and a stat change event for every total that changed, the
 * shield's included. They don't check permissions: a plugin acting for a player checks
 * that player's permission itself.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface Stats {

    /** Every stat, in stats.yml order. An unmodifiable list. */
    @NotNull List<Stat> all();

    /**
     * The stat with this id or alias, ignoring case.
     *
     * @param idOrAlias for example {@code "strength"}, {@code "str"} or {@code "hp"}
     * @return the stat, empty when stats.yml has none with that name
     */
    @NotNull Optional<Stat> find(@NotNull String idOrAlias);

    /**
     * A character's numbers for a stat.
     *
     * @return the values, empty when the character or the stat doesn't exist
     */
    @NotNull Optional<StatValues> get(@NotNull CharacterId character, @NotNull Stat stat);

    /**
     * Changes a character's stat, like its stat command. For a bar it changes the current value.
     *
     * @param amount how much; 0 or more, except for {@link StatAction#SET}
     * @return the change of the stat's total, empty when the character or the stat doesn't exist
     * @throws IllegalArgumentException when the amount is negative for an action other than
     *                                  SET, or the action is DAMAGE on a stat without a shield
     */
    @NotNull Optional<StatChange> apply(@NotNull CharacterId character, @NotNull Stat stat,
                                        @NotNull StatAction action, int amount);

    /** Same as {@link #get(CharacterId, Stat)}, with the stat's id or alias. */
    default @NotNull Optional<StatValues> get(@NotNull CharacterId character, @NotNull String statIdOrAlias) {
        return find(statIdOrAlias).flatMap(stat -> get(character, stat));
    }

    /** Same as {@link #apply(CharacterId, Stat, StatAction, int)}, with the stat's id or alias. */
    default @NotNull Optional<StatChange> apply(@NotNull CharacterId character, @NotNull String statIdOrAlias,
                                                @NotNull StatAction action, int amount) {
        return find(statIdOrAlias).flatMap(stat -> apply(character, stat, action, amount));
    }
}
