package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The conditions of conditions.yml, and what each character has now. A character has a
 * condition because it was given directly, or because one of its injuries or other
 * conditions gives it; {@link ActiveCondition} says which.
 *
 * <p>A condition is found by its id, ignoring case. {@link #types()}, {@link #type(String)}
 * and {@link #tag(String, int)} are safe from any thread. The others must be called on the
 * server's main thread, and throw {@link IllegalStateException} otherwise.
 *
 * <p>Writes work like {@code /condition}: saved straight away, the stats follow the
 * condition's effects, and the condition events fire with {@code ChangeCause.API}. They
 * don't check permissions.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 *
 * @since 1.2.0
 */
@ApiStatus.NonExtendable
public interface Conditions {

    /** Every condition, in conditions.yml order. An unmodifiable list. */
    @NotNull List<ConditionType> types();

    /**
     * The condition with this id, ignoring case.
     *
     * @return the condition, empty when conditions.yml has none with that id
     */
    @NotNull Optional<ConditionType> type(@NotNull String id);

    /**
     * The character's current conditions, in conditions.yml order: the direct ones and the
     * ones its injuries and conditions give. Empty for an unknown character.
     */
    @NotNull List<ActiveCondition> current(@NotNull CharacterId character);

    /**
     * Gives the character a condition directly, like {@code /condition award}. Giving it
     * again changes the level.
     *
     * @param level 1 or more
     * @return the condition as the character now has it (its level may be higher, when
     *         something else also gives it), empty when the character or the condition
     *         doesn't exist
     * @throws IllegalArgumentException when the level is under 1
     */
    @NotNull Optional<ActiveCondition> award(@NotNull CharacterId character, @NotNull String conditionId, int level);

    /**
     * Takes back a direct award, like {@code /condition remove}. A condition an injury or
     * another condition also gives stays.
     *
     * @return whether the character had it as a direct award
     */
    boolean remove(@NotNull CharacterId character, @NotNull String conditionId);

    /**
     * How the condition shows in chat ({@code [condition_x_N]}): its name, with the level
     * after it when above 1, and the description on hover with {@code $} as the level, in
     * the look of {@code condition.tag} in messages.yml.
     *
     * @param level 1 or more
     * @return the tag as MiniMessage, empty when conditions.yml has no such condition
     * @throws IllegalArgumentException when the level is under 1
     */
    @NotNull Optional<String> tag(@NotNull String conditionId, int level);
}
