package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The skills of skills.yml, their levels, and each character's XP in them.
 *
 * <p>A skill is found by its id or one of its aliases from skills.yml, ignoring case, the
 * same names {@code /skills addxp} takes. Nothing else matches.
 *
 * <p>The methods about skills.yml itself ({@link #all()}, {@link #find(String)},
 * {@link #levels()}, {@link #levelFor(int)}) are safe from any thread. The methods that
 * read or change a character must be called on the server's main thread, and throw
 * {@link IllegalStateException} otherwise.
 *
 * <p>Writes work like {@code /skills setxp} and {@code /skills addxp}: the XP is saved,
 * the LuckPerms level nodes of the player's active profile follow, and the skill change
 * event fires. They don't check permissions: a plugin acting for a player checks that
 * player's permission itself.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface Skills {

    /** Every skill, in skills.yml order. An unmodifiable list. */
    @NotNull List<Skill> all();

    /**
     * The skill with this id or alias, ignoring case.
     *
     * @param idOrAlias for example {@code "metalworking"}, {@code "metal"} or {@code "T&E"}
     * @return the skill, empty when skills.yml has none with that name
     */
    @NotNull Optional<Skill> find(@NotNull String idOrAlias);

    /** Every level, lowest first. An unmodifiable list, never empty. */
    @NotNull List<SkillLevel> levels();

    /**
     * The level this much XP reaches: the highest level whose {@code xp} it has. Below the
     * first level's XP it is the first level.
     *
     * @param xp the XP
     * @return the level
     */
    @NotNull SkillLevel levelFor(int xp);

    /**
     * A character's XP in a skill.
     *
     * @return the XP, empty when the character or the skill doesn't exist
     */
    @NotNull OptionalInt xp(@NotNull CharacterId character, @NotNull Skill skill);

    /**
     * A character's level in a skill, the one its XP reaches.
     *
     * @return the level, empty when the character or the skill doesn't exist
     */
    @NotNull Optional<SkillLevel> level(@NotNull CharacterId character, @NotNull Skill skill);

    /**
     * Sets a character's XP in a skill, like {@code /skills setxp}.
     *
     * @param xp the new XP, 0 or more
     * @return the change, empty when the character or the skill doesn't exist
     * @throws IllegalArgumentException when the XP is negative
     */
    @NotNull Optional<SkillChange> setXp(@NotNull CharacterId character, @NotNull Skill skill, int xp);

    /**
     * Adds XP to a character's skill, like {@code /skills addxp}. The total stops at
     * {@link Integer#MAX_VALUE}.
     *
     * @param amount the XP to add, 0 or more
     * @return the change, empty when the character or the skill doesn't exist
     * @throws IllegalArgumentException when the amount is negative
     */
    @NotNull Optional<SkillChange> addXp(@NotNull CharacterId character, @NotNull Skill skill, int amount);

    /** Same as {@link #xp(CharacterId, Skill)}, with the skill's id or alias. */
    default @NotNull OptionalInt xp(@NotNull CharacterId character, @NotNull String skillIdOrAlias) {
        return find(skillIdOrAlias).map(skill -> xp(character, skill)).orElse(OptionalInt.empty());
    }

    /** Same as {@link #level(CharacterId, Skill)}, with the skill's id or alias. */
    default @NotNull Optional<SkillLevel> level(@NotNull CharacterId character, @NotNull String skillIdOrAlias) {
        return find(skillIdOrAlias).flatMap(skill -> level(character, skill));
    }

    /** Same as {@link #setXp(CharacterId, Skill, int)}, with the skill's id or alias. */
    default @NotNull Optional<SkillChange> setXp(@NotNull CharacterId character, @NotNull String skillIdOrAlias,
                                                 int xp) {
        return find(skillIdOrAlias).flatMap(skill -> setXp(character, skill, xp));
    }

    /** Same as {@link #addXp(CharacterId, Skill, int)}, with the skill's id or alias. */
    default @NotNull Optional<SkillChange> addXp(@NotNull CharacterId character, @NotNull String skillIdOrAlias,
                                                 int amount) {
        return find(skillIdOrAlias).flatMap(skill -> addXp(character, skill, amount));
    }
}
