package org.skysongdev.skysongstats.storage;

import java.util.Objects;
import java.util.UUID;

// Identifies one character: the player plus the profile name.
public record ProfileKey(UUID player, String profile) {
    public ProfileKey {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(profile, "profile");
    }
}
