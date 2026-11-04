package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * One skill from skills.yml, as it was when it was looked up. A plugin may keep it: every
 * method that takes a skill looks it up again by its {@link #id()}, so after a
 * {@code /ssreload} that removed the skill those methods answer empty.
 *
 * @param id        the skill's key in skills.yml, for example {@code metalworking}
 * @param name      its {@code name}, plain text, for example {@code "Metalworking"}
 * @param shortName its {@code short-name}, plain text; the same as {@code name} when not set
 * @param color     its {@code color}, a MiniMessage tag such as {@code <gold>}
 * @param aliases   its {@code aliases}, the other names lookups accept
 */
public record Skill(@NotNull String id, @NotNull String name, @NotNull String shortName, @NotNull String color,
                    @NotNull List<String> aliases) {

    /**
     * @throws NullPointerException when any part is null
     */
    public Skill {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(shortName, "shortName");
        Objects.requireNonNull(color, "color");
        aliases = List.copyOf(aliases);
    }
}
