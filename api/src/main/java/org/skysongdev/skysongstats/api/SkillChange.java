package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One skill's XP before and after a change.
 *
 * @param character the character whose XP changed
 * @param skill     the skill
 * @param before    the XP before the change
 * @param after     the XP after the change
 */
public record SkillChange(@NotNull CharacterId character, @NotNull Skill skill, int before, int after) {

    /**
     * @throws NullPointerException when the character or the skill is null
     */
    public SkillChange {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(skill, "skill");
    }

    /** Whether the XP is different afterwards. */
    public boolean changed() {
        return before != after;
    }
}
