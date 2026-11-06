package org.skysongdev.skysongstats.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongstats.api.ItemSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

// The item sources other plugins registered through the API: containers whose items
// count toward item-tags as if they were in the player's inventory. A source that fails
// counts nothing; its first failure is logged, the rest are not, so a broken source
// can't flood the log on every inventory change.
public final class ItemSourceRegistry {
    private final Map<Key, Entry> entries = new LinkedHashMap<>();
    private final Consumer<String> warn;

    public ItemSourceRegistry(Consumer<String> warn) {
        this.warn = Objects.requireNonNull(warn, "warn");
    }

    // Adds a source, empty when the plugin already has one with that id.
    public Optional<Entry> add(Plugin owner, String id, ItemSource source) {
        Key key = new Key(owner.getName(), id);
        if (entries.containsKey(key)) {
            return Optional.empty();
        }
        Entry entry = new Entry(owner, id, source);
        entries.put(key, entry);
        return Optional.of(entry);
    }

    // Whether the entry was still registered.
    public boolean remove(Entry entry) {
        if (!entry.registered) {
            return false;
        }
        entry.registered = false;
        entries.remove(new Key(entry.owner.getName(), entry.id));
        return true;
    }

    // Removes every source of the plugin; whether it had any.
    public boolean removeOwner(Plugin owner) {
        List<Entry> owned = entries.values().stream().filter(entry -> entry.owner.equals(owner)).toList();
        owned.forEach(this::remove);
        return !owned.isEmpty();
    }

    public void clear() {
        List.copyOf(entries.values()).forEach(this::remove);
    }

    // Every source's items for the player, in registration order.
    public List<ItemStack> items(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (Entry entry : entries.values()) {
            items.addAll(entry.read(player));
        }
        return items;
    }

    private record Key(String plugin, String id) {
    }

    public final class Entry {
        private final Plugin owner;
        private final String id;
        private final ItemSource source;
        private boolean registered = true;
        private boolean failureLogged;

        private Entry(Plugin owner, String id, ItemSource source) {
            this.owner = owner;
            this.id = id;
            this.source = source;
        }

        public Plugin owner() {
            return owner;
        }

        public String id() {
            return id;
        }

        public boolean isRegistered() {
            return registered;
        }

        // The source's items, or none when it throws or answers null. Catching every
        // RuntimeException is deliberate: another plugin's bug must not stop the recount.
        private Collection<? extends ItemStack> read(Player player) {
            try {
                Collection<? extends ItemStack> items = source.items(player);
                if (items != null) {
                    return items;
                }
                logOnce(player, "returned null");
            } catch (RuntimeException e) {
                logOnce(player, "threw " + e);
            }
            return List.of();
        }

        private void logOnce(Player player, String what) {
            if (!failureLogged) {
                failureLogged = true;
                warn.accept("Item source '" + id + "' of " + owner.getName() + " " + what + " for " + player.getName()
                        + "; it counts nothing when that happens. Further failures of this source are not logged.");
            }
        }
    }
}
