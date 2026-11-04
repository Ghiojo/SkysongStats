package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One proficiency tier from the {@code tiers:} list of proficiencies.yml. Every
 * proficiency starts at the first tier.
 *
 * @param id    its key, for example {@code proficient}; also the last part of its
 *              LuckPerms node {@code skysongstats.proficiency.<proficiency>.<tier>}
 * @param name  its name, plain text
 * @param color its color, a MiniMessage tag
 */
public record Tier(@NotNull String id, @NotNull String name, @NotNull String color) {

    /**
     * @throws NullPointerException when any part is null
     */
    public Tier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
    }
}
