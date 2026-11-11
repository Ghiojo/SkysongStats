package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * One of an ancestry's {@code choices:}, made with {@link AncestryPicks}. It is either
 * {@link Stats} (the same amount to a number of stats) or {@link Packages} (one of a few
 * sets of adjustments).
 *
 * @since 1.4.0
 */
public sealed interface AncestryChoice permits AncestryChoice.Stats, AncestryChoice.Packages {

    /** Its name from ancestries.yml, MiniMessage; empty when the file gives none. */
    @NotNull String name();

    /**
     * Whether its picks may land on a stat the ancestry already moved the same way. False
     * by default: as in Pathfinder 2e, an ancestry boosts a stat at most once.
     */
    boolean stack();

    /**
     * Adds (or takes) {@code amount} to {@code picks} different stats.
     *
     * @param name     its name, MiniMessage; empty when the file gives none
     * @param amount   what each picked stat gets; never 0
     * @param picks    how many different stats are picked
     * @param from     the stat ids it may pick from; every /setup stat when {@code anyStat}
     * @param anyStat  whether it may pick any stat with a {@code setup:} section
     * @param stack    see {@link AncestryChoice#stack()}
     */
    record Stats(@NotNull String name, int amount, int picks, @NotNull List<String> from, boolean anyStat,
                 boolean stack) implements AncestryChoice {

        /**
         * @throws NullPointerException when any part is null
         */
        public Stats {
            Objects.requireNonNull(name, "name");
            from = List.copyOf(from);
        }
    }

    /**
     * Gives one of its packages.
     *
     * @param name    its name, MiniMessage; empty when the file gives none
     * @param options the packages, in file order; picked by their position in this list
     * @param stack   see {@link AncestryChoice#stack()}
     */
    record Packages(@NotNull String name, @NotNull List<AncestryPackage> options, boolean stack)
            implements AncestryChoice {

        /**
         * @throws NullPointerException when any part is null
         */
        public Packages {
            Objects.requireNonNull(name, "name");
            options = List.copyOf(options);
        }
    }
}
