package org.skysongdev.skysongstats.items;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// Recounts the player's Skysong item tags into their active character whenever
// the inventory changes, they join, or they switch profile. The counts are added
// to the stats that list those tags (see ItemTagCounter) and are never saved.
public final class ItemTagListener implements Listener {
    private final StatService stats;
    private final Function<Player, Optional<ProfileKey>> activeCharacter;

    public ItemTagListener(StatService stats, Function<Player, Optional<ProfileKey>> activeCharacter) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.activeCharacter = Objects.requireNonNull(activeCharacter, "activeCharacter");
    }

    @EventHandler
    public void onInventoryChange(PlayerInventorySlotChangeEvent event) {
        recount(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        recount(event.getPlayer());
    }

    @EventHandler
    public void onProfileSwitch(ProfileUpdateEvent event) {
        // /profile switch fires this after the new profile is active.
        if (event.getPlayer() != null) {
            recount(event.getPlayer());
        }
    }

    // Does nothing for a player without a loaded character.
    public void recount(Player player) {
        activeCharacter.apply(player)
                .filter(character -> stats.sheet(character).isPresent())
                .ifPresent(character -> stats.setItemCounts(character, ItemTagCounter.count(
                        player.getInventory().getContents(), ItemTagCounter.sources(stats.definitions()))));
    }
}
