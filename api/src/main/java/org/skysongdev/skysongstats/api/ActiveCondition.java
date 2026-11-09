package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A condition a character has now, and why. It lasts until every source is gone.
 *
 * @param typeId         the condition's id in conditions.yml; {@link Conditions#type(String)}
 *                       gives the rest
 * @param level          its level, 1 or more: the highest of all its sources
 * @param direct         whether it was given directly ({@code /condition award} or
 *                       {@link Conditions#award(CharacterId, String, int)}); only a direct
 *                       award can be removed
 * @param fromInjuries   the ids of the character's injuries that give it
 * @param fromConditions the ids of the character's other conditions that give it
 * @since 1.2.0
 */
public record ActiveCondition(@NotNull String typeId, int level, boolean direct, @NotNull List<String> fromInjuries,
                              @NotNull List<String> fromConditions) {

    /**
     * @throws NullPointerException     when any part is null
     * @throws IllegalArgumentException when the level is under 1
     */
    public ActiveCondition {
        Objects.requireNonNull(typeId, "typeId");
        if (level < 1) {
            throw new IllegalArgumentException("A condition's level is at least 1, got " + level);
        }
        fromInjuries = List.copyOf(fromInjuries);
        fromConditions = List.copyOf(fromConditions);
    }
}
