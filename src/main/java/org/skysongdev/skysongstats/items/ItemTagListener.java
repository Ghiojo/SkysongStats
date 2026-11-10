package org.skysongdev.skysongstats.items;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.api.event.ProfileUpdateEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// Recounts the player's Skysong item tags into their active character whenever
// the inventory changes, they join, or they switch profile. The items counted are the
// inventory plus what other plugins' item sources hold for the player (see
// ItemSourceRegistry). The counts are added to the stats that list those tags (see
// ItemTagCounter) and are never saved.
public final class ItemTagListener implements Listener {
    private final StatService stats;
    private final Function<Player, Optional<ProfileKey>> activeCharacter;
    private final ItemTagReader reader;
    private final Function<Player, ? extends Collection<? extends ItemStack>> extraItems;

    public ItemTagListener(StatService stats, Function<Player, Optional<ProfileKey>> activeCharacter,
                           ItemTagReader reader, Function<Player, ? extends Collection<? extends ItemStack>> extraItems) {
        this.stats = Objects.requireNonNull(stats, "stats");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.extraItems = Objects.requireNonNull(extraItems, "extraItems");
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
        recount(event.getPlayer());
    }

    // Does nothing for a player without a loaded character.
    public void recount(Player player) {
        activeCharacter.apply(player)
                .filter(character -> stats.sheet(character).isPresent())
                .ifPresent(character -> count(character, items(player)));
    }

    // Counts these items as everything the character carries, for characters
    // without a player behind them (another plugin's NPCs). Does nothing for a
    // character without stats.
    public void count(ProfileKey character, List<ItemStack> items) {
        if (stats.sheet(character).isPresent()) {
            stats.setItemCounts(character, ItemTagCounter.count(reader, items,
                    ItemTagCounter.sources(stats.definitions())));
        }
    }

    private List<ItemStack> items(Player player) {
        List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        items.addAll(extraItems.apply(player));
        return items;
    }
}
