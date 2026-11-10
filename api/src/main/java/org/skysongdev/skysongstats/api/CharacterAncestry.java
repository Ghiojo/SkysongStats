package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A character's ancestry, and what it gave the character's base stats. What it gave is
 * kept as it was on the day: editing ancestries.yml later doesn't change it.
 *
 * @param id             the ancestry's id
 * @param name           its name, MiniMessage; its id when it has left ancestries.yml
 * @param choicesPending whether its choices are still to be made
 *                       ({@link Ancestries#choose(CharacterId, AncestryPicks)})
 * @param adjustments    everything it gave, fixed ones first
 * @since 1.4.0
 */
public record CharacterAncestry(@NotNull String id, @NotNull String name, boolean choicesPending,
                                @NotNull List<Given> adjustments) {

    /**
     * @throws NullPointerException when any part is null
     */
    public CharacterAncestry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        adjustments = List.copyOf(adjustments);
    }

    /** What it gave one stat, in all; 0 when nothing. */
    public int total(@NotNull String stat) {
        Objects.requireNonNull(stat, "stat");
        return adjustments.stream().filter(given -> given.stat().equalsIgnoreCase(stat)).mapToInt(Given::amount).sum();
    }

    /**
     * One adjustment the character got.
     *
     * @param stat   the stat's id
     * @param amount what it added (positive) or took (negative)
     * @param choice the position of the choice that gave it, from 0; -1 for the ancestry's
     *               fixed adjustments
     */
    public record Given(@NotNull String stat, int amount, int choice) {

        /**
         * @throws NullPointerException when the stat is null
         */
        public Given {
            Objects.requireNonNull(stat, "stat");
        }

        /** Whether it is one of the ancestry's fixed adjustments rather than a choice's. */
        public boolean fixed() {
            return choice < 0;
        }
    }
}
