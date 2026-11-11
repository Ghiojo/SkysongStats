package org.skysongdev.skysongstats.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The ancestries of ancestries.yml, and each character's ancestry: for player characters
 * and plugin characters (NPCs) alike.
 *
 * <p>An ancestry's adjustments go into the character's <b>base</b> stats, and what it gave
 * is saved, so a change always takes off exactly what was given before. The changes work
 * like {@code /ancestry}: saved straight away, the character's ancestry field follows, the
 * stat events fire, and {@link org.skysongdev.skysongstats.api.event.AncestryChangeEvent}
 * fires with {@code ChangeCause.API}. They don't check the ancestry's {@code permission:};
 * a plugin acting for a player checks {@link #mayPick(Player, Ancestry)} itself.
 *
 * <p>Invalid picks (a stat the choice can't pick, too few picks, a package that doesn't
 * exist, a boost on a stat already boosted...) throw {@link IllegalArgumentException}
 * naming every problem, changing nothing; {@link #problems(Ancestry, AncestryPicks)} checks
 * first.
 *
 * <p>The methods about ancestries.yml itself ({@link #all()}, {@link #ancestry(String)},
 * {@link #mayPick(Player, Ancestry)}, {@link #problems(Ancestry, AncestryPicks)}) are safe
 * from any thread. The others must be called on the server's main thread, and throw
 * {@link IllegalStateException} otherwise.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 *
 * @since 1.4.0
 */
@ApiStatus.NonExtendable
public interface Ancestries {

    /** Every ancestry, in ancestries.yml order. An unmodifiable list. */
    @NotNull List<Ancestry> all();

    /**
     * The ancestry with this id or name, ignoring case and formatting.
     *
     * @return the ancestry, empty when ancestries.yml has none by that id or name
     */
    @NotNull Optional<Ancestry> ancestry(@NotNull String idOrName);

    /** Whether the player may pick it in /setup: it has no {@code permission:}, or they have the node. */
    boolean mayPick(@NotNull Player player, @NotNull Ancestry ancestry);

    /**
     * Why these picks don't fit the ancestry's choices, in English, one entry per problem.
     *
     * @return the problems; empty when the picks are fine. An ancestry that has left
     *         ancestries.yml has one problem saying so
     */
    @NotNull List<String> problems(@NotNull Ancestry ancestry, @NotNull AncestryPicks picks);

    /** The character's ancestry; empty when it has none, or the character doesn't exist. */
    @NotNull Optional<CharacterAncestry> of(@NotNull CharacterId character);

    /**
     * Gives the character the ancestry with its choices made, like /setup does: takes off
     * everything its old ancestry gave, then puts on the new one. Giving the ancestry it
     * already has puts its current version from ancestries.yml on.
     *
     * @return the change; empty when the character doesn't exist or the ancestry has left
     *         ancestries.yml
     * @throws IllegalArgumentException when the picks don't fit the ancestry
     */
    @NotNull Optional<AncestryChange> set(@NotNull CharacterId character, @NotNull Ancestry ancestry,
                                          @NotNull AncestryPicks picks);

    /**
     * Gives the character the ancestry like {@code /ancestry set}: its fixed adjustments
     * now, its choices left pending for {@link #choose(CharacterId, AncestryPicks)}. The
     * player, if online and playing the character, is told to make them.
     *
     * @return the change; empty when the character doesn't exist or the ancestry has left
     *         ancestries.yml
     */
    @NotNull Optional<AncestryChange> set(@NotNull CharacterId character, @NotNull Ancestry ancestry);

    /**
     * Makes the character's pending choices.
     *
     * @return the change; empty when the character has no pending choices, doesn't exist, or
     *         its ancestry has left ancestries.yml
     * @throws IllegalArgumentException when the picks don't fit its ancestry
     */
    @NotNull Optional<AncestryChange> choose(@NotNull CharacterId character, @NotNull AncestryPicks picks);

    /**
     * Takes off what the character's choices gave and makes them pending again, like
     * {@code /ancestry rechoose}; the fixed adjustments stay.
     *
     * @return the change; empty when the character has no ancestry, its ancestry has no
     *         choices or has left ancestries.yml
     */
    @NotNull Optional<AncestryChange> rechoose(@NotNull CharacterId character);

    /**
     * Takes off everything the ancestry gave, like {@code /ancestry remove}. The character's
     * ancestry field is free text again.
     *
     * @return the change; empty when the character had no ancestry
     */
    @NotNull Optional<AncestryChange> remove(@NotNull CharacterId character);

    /**
     * Opens the choices page of {@code /ancestry choose} for the player's active character,
     * when it has choices pending. Confirming makes them, with {@code ChangeCause.API}.
     *
     * @return whether the page opened
     */
    boolean openChoices(@NotNull Player player);
}
