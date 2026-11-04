package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One level from the {@code levels:} list of skills.yml. A skill is at the highest level
 * whose {@link #minXp()} its XP reaches.
 *
 * @param id    the level's key in skills.yml, for example {@code novice}
 * @param name  its {@code name}, plain text, for example {@code "Novice"}
 * @param color its {@code color}, a MiniMessage tag such as {@code <blue>}
 * @param minXp its {@code xp}: the XP needed to reach it
 */
public record SkillLevel(@NotNull String id, @NotNull String name, @NotNull String color, int minXp) {

    /**
     * @throws NullPointerException when the id, name or color is null
     */
    public SkillLevel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
    }
}
