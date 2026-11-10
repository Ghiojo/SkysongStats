package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * One character: a player and one of their profiles, such as {@code Default}. Every
 * SkysongStats value (stats, skills, proficiencies, downtime, the character sheet) belongs
 * to a character. Get the one a player is playing with {@link Profiles#active(UUID)}.
 *
 * <p>The profile name is matched exactly, with its case, as {@code /profile} shows it.
 *
 * <p>A character another plugin owns, such as an NPC (see {@link Characters}), uses the id
 * and profile name that plugin gave it, in place of a player's UUID and profile.
 *
 * @param player  the player's UUID, or a plugin character's id
 * @param profile the profile's name, or a plugin character's profile name
 */
public record CharacterId(@NotNull UUID player, @NotNull String profile) {

    /**
     * @throws NullPointerException when the player or the profile is null
     */
    public CharacterId {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(profile, "profile");
    }
}
