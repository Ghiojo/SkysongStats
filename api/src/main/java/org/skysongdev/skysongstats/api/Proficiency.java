package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * One proficiency from proficiencies.yml, as it was when it was looked up. A plugin may
 * keep it: every method that takes a proficiency looks it up again by its {@link #id()}.
 *
 * @param id      its key in proficiencies.yml, for example {@code heavy_armor}
 * @param name    its name, plain text
 * @param color   its color, a MiniMessage tag
 * @param skillId the id of the skill it belongs to
 * @param aliases the other names lookups accept
 */
public record Proficiency(@NotNull String id, @NotNull String name, @NotNull String color, @NotNull String skillId,
                          @NotNull List<String> aliases) {

    /**
     * @throws NullPointerException when any part is null
     */
    public Proficiency {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(skillId, "skillId");
        aliases = List.copyOf(aliases);
    }
}
