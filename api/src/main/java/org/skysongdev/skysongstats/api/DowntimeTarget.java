package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Which downtime pool a change is for: a player's global downtime or one character's.
 *
 * <pre>{@code
 * downtime.add(DowntimeTarget.global(player.getUniqueId()), 2);
 * downtime.add(DowntimeTarget.character(character), 1);
 * }</pre>
 */
public sealed interface DowntimeTarget {

    /** The player whose downtime it is. */
    @NotNull UUID player();

    /** Which pool it is. */
    @NotNull DowntimePool pool();

    /** A player's global downtime. */
    static @NotNull DowntimeTarget global(@NotNull UUID player) {
        return new Global(player);
    }

    /** One character's downtime. */
    static @NotNull DowntimeTarget character(@NotNull CharacterId character) {
        return new Character(character);
    }

    /**
     * A player's global downtime.
     *
     * @param player the player
     */
    record Global(@NotNull UUID player) implements DowntimeTarget {
        /** @throws NullPointerException when the player is null */
        public Global {
            Objects.requireNonNull(player, "player");
        }

        @Override
        public @NotNull DowntimePool pool() {
            return DowntimePool.GLOBAL;
        }
    }

    /**
     * One character's downtime.
     *
     * @param character the character
     */
    record Character(@NotNull CharacterId character) implements DowntimeTarget {
        /** @throws NullPointerException when the character is null */
        public Character {
            Objects.requireNonNull(character, "character");
        }

        @Override
        public @NotNull UUID player() {
            return character.player();
        }

        @Override
        public @NotNull DowntimePool pool() {
            return DowntimePool.CHARACTER;
        }
    }
}
