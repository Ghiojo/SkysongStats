package org.skysongdev.skysongstats.skill;

import java.util.Set;
import java.util.UUID;

// Gives and takes permission nodes on a player's account (LuckPerms on the server).
@FunctionalInterface
public interface PlayerNodes {
    // Adds every node in grant and removes every node in revoke, then saves.
    void update(UUID player, Set<String> grant, Set<String> revoke);
}
