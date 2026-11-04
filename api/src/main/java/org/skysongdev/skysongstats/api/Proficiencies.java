package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The proficiencies and tiers of proficiencies.yml, and each character's tiers, focus and
 * training.
 *
 * <p>A proficiency is found by its id or one of its aliases, ignoring case; a tier by its
 * id, ignoring case. Nothing else matches.
 *
 * <p>The methods about proficiencies.yml itself ({@link #all()}, {@link #find(String)},
 * {@link #ofSkill(Skill)}, {@link #tiers()}, {@link #findTier(String)}) are safe from any
 * thread. The methods that read or change a character must be called on the server's main
 * thread, and throw {@link IllegalStateException} otherwise.
 *
 * <p>Writes work like {@code /proficiency}: saved straight away, the LuckPerms tier nodes
 * of the player's active profile follow, and the proficiency events fire. They don't check
 * permissions: a plugin acting for a player checks that player's permission itself.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface Proficiencies {

    /** Every proficiency, in proficiencies.yml order. An unmodifiable list. */
    @NotNull List<Proficiency> all();

    /**
     * The proficiency with this id or alias, ignoring case.
     *
     * @return the proficiency, empty when proficiencies.yml has none with that name
     */
    @NotNull Optional<Proficiency> find(@NotNull String idOrAlias);

    /** The proficiencies of one skill, in proficiencies.yml order. An unmodifiable list. */
    @NotNull List<Proficiency> ofSkill(@NotNull Skill skill);

    /** Every tier, lowest first. An unmodifiable list with at least two tiers. */
    @NotNull List<Tier> tiers();

    /**
     * The tier with this id, ignoring case.
     *
     * @return the tier, empty when proficiencies.yml has none with that id
     */
    @NotNull Optional<Tier> findTier(@NotNull String id);

    /**
     * A character's tier in a proficiency.
     *
     * @return the tier, empty when the character or the proficiency doesn't exist
     */
    @NotNull Optional<Tier> tier(@NotNull CharacterId character, @NotNull Proficiency proficiency);

    /**
     * The proficiency a character is training.
     *
     * @return the focus, empty when the character has none or doesn't exist
     */
    @NotNull Optional<Focus> focus(@NotNull CharacterId character);

    /**
     * The points a character's focus needs to reach its next tier.
     *
     * @return the points, empty when the character has no focus or doesn't exist
     */
    @NotNull OptionalInt pointsNeeded(@NotNull CharacterId character);

    /**
     * Focuses a proficiency with 0 points, like {@code /proficiency focus}.
     *
     * @param discardPoints whether the current focus's points may be lost; when false and
     *                      it has points, nothing changes and the answer is
     *                      {@link FocusResult#NEEDS_CONFIRMATION}
     * @return what happened, empty when the character or the proficiency doesn't exist
     */
    @NotNull Optional<FocusResult> setFocus(@NotNull CharacterId character, @NotNull Proficiency proficiency,
                                            boolean discardPoints);

    /**
     * A trainer's lesson, like {@code /proficiency points add}: adds points to the focus
     * when it belongs to the skill and is below the trainer's maximum tier. Reaching the next
     * tier's cost raises the tier and puts the points back at 0.
     *
     * @param points  the points, 1 or more
     * @param maxTier the highest tier the trainer can teach
     * @return what the lesson did, empty when the character, the skill or the tier doesn't exist
     * @throws IllegalArgumentException when the points are under 1
     */
    @NotNull Optional<TrainingResult> train(@NotNull CharacterId character, @NotNull Skill skill, int points,
                                            @NotNull Tier maxTier);

    /**
     * Sets a character's tier in a proficiency, up or down, like {@code /proficiency tier set}.
     * The focus keeps its points, unless the focused proficiency reaches the top tier: then
     * the focus is cleared.
     *
     * @return the change, empty when the character, the proficiency or the tier doesn't exist
     */
    @NotNull Optional<TierChange> setTier(@NotNull CharacterId character, @NotNull Proficiency proficiency,
                                          @NotNull Tier tier);

    /**
     * Sets the focus's points, like {@code /proficiency points set}. They must stay below
     * {@link #pointsNeeded(CharacterId)}: tier changes go through
     * {@link #setTier(CharacterId, Proficiency, Tier)}.
     *
     * @return the focus afterwards, empty when the character has no focus or doesn't exist
     * @throws IllegalArgumentException when the points are negative or reach the next tier's cost
     */
    @NotNull Optional<Focus> setPoints(@NotNull CharacterId character, int points);

    /**
     * Takes points off the focus, stopping at 0, like {@code /proficiency points remove}.
     * Never lowers a tier.
     *
     * @return the focus afterwards, empty when the character has no focus or doesn't exist
     * @throws IllegalArgumentException when the points are negative
     */
    @NotNull Optional<Focus> removePoints(@NotNull CharacterId character, int points);
}
