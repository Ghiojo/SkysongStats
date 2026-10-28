package org.skysongdev.skysongstats.service.downtime;

import java.util.Objects;
import java.util.UUID;

/** Identifies one profile's character downtime: the player plus the profile name. */
public record CharacterKey(UUID player, String profile) {
    public CharacterKey {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(profile, "profile");
    }
}
