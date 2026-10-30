package org.skysongdev.skysongstats.commands.proficiency;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

// The focus switches waiting for /proficiency focus confirm, one per player. A
// switch is forgotten after a minute, when the player logs out, or when they
// confirm on another profile than the one that asked.
public final class PendingFocusChanges implements Listener {
    static final long TIMEOUT_MILLIS = 60_000;

    private record Pending(ProfileKey character, String proficiencyId, long expiresAt) {
    }

    private final LongSupplier clock;
    private final Map<UUID, Pending> pending = new HashMap<>();

    // clock gives the current time in milliseconds.
    public PendingFocusChanges(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // Replaces any switch the player was already asked about.
    public void put(ProfileKey character, String proficiencyId) {
        pending.put(character.player(), new Pending(character, proficiencyId, clock.getAsLong() + TIMEOUT_MILLIS));
    }

    // The proficiency waiting for this character, removed so it can only be
    // confirmed once. Empty when there is none or it ran out.
    public Optional<String> take(ProfileKey character) {
        Pending waiting = pending.remove(character.player());
        if (waiting == null || !waiting.character().equals(character) || clock.getAsLong() > waiting.expiresAt()) {
            return Optional.empty();
        }
        return Optional.of(waiting.proficiencyId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
