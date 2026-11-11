package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What was picked for each choice of an ancestry. Choices are named by their position in
 * {@link Ancestry#choices()} and packages by their position in
 * {@link AncestryChoice.Packages#options()}, both from 0:
 *
 * <pre>{@code
 * AncestryPicks picks = AncestryPicks.builder().stats(0, "dex").stats(1, "str", "con").option(2, 0).build();
 * }</pre>
 *
 * Whether the picks fit the ancestry is checked when they are used, with the rules of the
 * choices page; {@link Ancestries#problems(Ancestry, AncestryPicks)} checks first.
 * Immutable.
 *
 * @since 1.4.0
 */
public final class AncestryPicks {
    private static final AncestryPicks NONE = new AncestryPicks(Map.of(), Map.of());

    private final Map<Integer, List<String>> stats;
    private final Map<Integer, Integer> options;

    private AncestryPicks(Map<Integer, List<String>> stats, Map<Integer, Integer> options) {
        this.stats = Collections.unmodifiableMap(new TreeMap<>(stats));
        this.options = Collections.unmodifiableMap(new TreeMap<>(options));
    }

    /** No picks, for an ancestry without choices. */
    public static @NotNull AncestryPicks none() {
        return NONE;
    }

    /** Starts a new set of picks. */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /** The positions of the choices something was picked for, in order. */
    public @NotNull Set<Integer> choices() {
        Set<Integer> choices = new TreeSet<>(stats.keySet());
        choices.addAll(options.keySet());
        return Collections.unmodifiableSet(choices);
    }

    /** The stats picked for a {@link AncestryChoice.Stats} choice; empty when none were. */
    public @NotNull Optional<List<String>> stats(int choice) {
        return Optional.ofNullable(stats.get(choice));
    }

    /** The package picked for a {@link AncestryChoice.Packages} choice; empty when none was. */
    public @NotNull OptionalInt option(int choice) {
        Integer option = options.get(choice);
        return option == null ? OptionalInt.empty() : OptionalInt.of(option);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AncestryPicks picks && stats.equals(picks.stats) && options.equals(picks.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stats, options);
    }

    @Override
    public String toString() {
        return "AncestryPicks{stats=" + stats + ", options=" + options + "}";
    }

    /** Builds {@link AncestryPicks}. A later pick for the same choice replaces the earlier one. */
    public static final class Builder {
        private final Map<Integer, List<String>> stats = new TreeMap<>();
        private final Map<Integer, Integer> options = new TreeMap<>();

        private Builder() {
        }

        /**
         * Picks stats, by id or alias, for a {@link AncestryChoice.Stats} choice.
         *
         * @throws IllegalArgumentException when the position is negative
         */
        public @NotNull Builder stats(int choice, @NotNull String... picked) {
            requirePosition(choice, "choice");
            List<String> copy = List.of(picked);
            options.remove(choice);
            stats.put(choice, copy);
            return this;
        }

        /**
         * Picks a package for a {@link AncestryChoice.Packages} choice.
         *
         * @throws IllegalArgumentException when either position is negative
         */
        public @NotNull Builder option(int choice, int option) {
            requirePosition(choice, "choice");
            requirePosition(option, "option");
            stats.remove(choice);
            options.put(choice, option);
            return this;
        }

        /** The picks. */
        public @NotNull AncestryPicks build() {
            return stats.isEmpty() && options.isEmpty() ? NONE : new AncestryPicks(stats, options);
        }

        private static void requirePosition(int position, String what) {
            if (position < 0) {
                throw new IllegalArgumentException(what + " positions start at 0, got " + position);
            }
        }
    }
}
