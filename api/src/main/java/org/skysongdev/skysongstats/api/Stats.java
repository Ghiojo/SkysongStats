package org.skysongdev.skysongstats.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The stats of stats.yml and each character's values, bars included.
 *
 * <p>A stat is found by its id, one of its aliases from stats.yml, or its short name
 * (from stats.yml or {@link #assignShortName assigned}, when it has no spaces), ignoring
 * case: the same names the stat commands take. Nothing else matches.
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
     * The stat with this id, alias or short name, ignoring case.
     *
     * @param idOrAlias for example {@code "strength"}, {@code "str"}, {@code "thp"} or {@code "hp"}
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

    /**
     * The stat modifiers a character has from its current injuries and conditions, injuries
     * first, the same list {@code /modifier view} shows below the saved modifiers. They count
     * in {@link #get(CharacterId, Stat)}'s totals and modifiers like saved ones do.
     *
     * @return the effects, empty when the character has none or doesn't exist
     * @since 1.2.0
     */
    @NotNull List<Effect> effects(@NotNull CharacterId character);

    /**
     * Gives a stat a short name while the plugin is enabled, over its {@code short-name} in
     * stats.yml. It shows everywhere the short name does (messages, sheets, placeholders,
     * {@link Stat#shortName()}), and commands accept it like an alias when it has no spaces.
     *
     * <p>Nothing is saved: the assignment lasts until it is {@link ShortNameAssignment#unassign()
     * unassigned} or the plugin disables, and survives {@code /ssreload}. A plugin that wants it
     * every time assigns it again in {@code onEnable}. When several assignments exist for one
     * stat, the newest is in effect; taking it off brings back the one before it, then stats.yml.
     * A {@link org.skysongdev.skysongstats.api.event.StatShortNameChangeEvent} fires when the
     * short name the stat shows changes. Must be called on the server's main thread.
     *
     * @param owner     the plugin assigning it, which must be enabled
     * @param stat      the stat
     * @param shortName the short name, plain text, for example {@code "Mgt"}
     * @return the assignment, empty (and nothing changes) when the stat is no longer in stats.yml,
     *         the short name is blank, or commands would read it as another stat's id, alias or
     *         short name
     * @throws IllegalStateException when the plugin is not enabled, or called from another thread
     * @since 1.5.0
     */
    @NotNull Optional<ShortNameAssignment> assignShortName(@NotNull Plugin owner, @NotNull Stat stat,
                                                           @NotNull String shortName);

    /**
     * Every short name plugins assigned and haven't taken off, oldest first, including those
     * not in effect ({@link ShortNameAssignment#isActive()}). An unmodifiable list. Must be
     * called on the server's main thread.
     *
     * @throws IllegalStateException when called from another thread
     * @since 1.5.0
     */
    @NotNull List<ShortNameAssignment> shortNameAssignments();

    /**
     * Same as {@link #assignShortName(Plugin, Stat, String)}, with the stat's id, alias or short name.
     *
     * @since 1.5.0
     */
    default @NotNull Optional<ShortNameAssignment> assignShortName(@NotNull Plugin owner, @NotNull String statIdOrAlias,
                                                                   @NotNull String shortName) {
        return find(statIdOrAlias).flatMap(stat -> assignShortName(owner, stat, shortName));
    }

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
