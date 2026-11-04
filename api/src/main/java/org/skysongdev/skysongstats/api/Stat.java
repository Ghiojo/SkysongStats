package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * One stat from stats.yml, as it was when it was looked up. A plugin may keep it: every
 * method that takes a stat looks it up again by its {@link #id()}, so after a
 * {@code /ssreload} that removed the stat those methods answer empty.
 *
 * <p>Two stats are equal when every part is equal.
 */
public final class Stat {
    private final String id;
    private final String name;
    private final String color;
    private final List<String> aliases;
    private final StatType type;
    private final Integer minimum;
    private final String shield;

    /**
     * @param id      the stat's key in stats.yml, for example {@code hp}
     * @param name    its display name, plain text
     * @param color   its color, a MiniMessage tag such as {@code <red>}
     * @param aliases the other names lookups accept
     * @param type    a plain value or a bar
     * @param minimum the lowest its total can go, null when it has no floor
     * @param shield  the id of the stat that absorbs damage first, null when none
     * @throws NullPointerException when the id, name, color, aliases or type is null
     */
    public Stat(@NotNull String id, @NotNull String name, @NotNull String color, @NotNull List<String> aliases,
                @NotNull StatType type, @Nullable Integer minimum, @Nullable String shield) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.color = Objects.requireNonNull(color, "color");
        this.aliases = List.copyOf(aliases);
        this.type = Objects.requireNonNull(type, "type");
        this.minimum = minimum;
        this.shield = shield;
    }

    /** The stat's key in stats.yml, for example {@code hp}. */
    public @NotNull String id() {
        return id;
    }

    /** Its display name, plain text. */
    public @NotNull String name() {
        return name;
    }

    /** Its color, a MiniMessage tag such as {@code <red>}. */
    public @NotNull String color() {
        return color;
    }

    /** The other names lookups accept. An unmodifiable list. */
    public @NotNull List<String> aliases() {
        return aliases;
    }

    /** A plain value or a bar. */
    public @NotNull StatType type() {
        return type;
    }

    /** Whether it is a bar. */
    public boolean isBar() {
        return type == StatType.BAR;
    }

    /** The name of a bar's max, {@code max_<id>}, the target of max modifiers. Empty for plain values. */
    public @NotNull Optional<String> maxId() {
        return isBar() ? Optional.of("max_" + id) : Optional.empty();
    }

    /** The lowest its total can go, empty when it has no floor. */
    public @NotNull OptionalInt minimum() {
        return minimum == null ? OptionalInt.empty() : OptionalInt.of(minimum);
    }

    /** The id of the stat that absorbs {@link StatAction#DAMAGE} first, empty when none. */
    public @NotNull Optional<String> shield() {
        return Optional.ofNullable(shield);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Stat stat && parts().equals(stat.parts());
    }

    @Override
    public int hashCode() {
        return parts().hashCode();
    }

    private List<Object> parts() {
        return Arrays.asList(id, name, color, aliases, type, minimum, shield);
    }

    @Override
    public String toString() {
        return "Stat[" + id + "]";
    }
}
