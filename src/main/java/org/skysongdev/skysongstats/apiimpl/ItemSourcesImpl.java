package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.api.ItemSource;
import org.skysongdev.skysongstats.api.ItemSourceRegistration;
import org.skysongdev.skysongstats.api.ItemSources;
import org.skysongdev.skysongstats.items.ItemSourceRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// Item sources kept in the registry the item tag listener reads. Every change to the
// sources recounts the online players, so the stats show the new total straight away.
// A plugin's sources go when it disables (the API offers this as a listener).
final class ItemSourcesImpl implements ItemSources, Listener {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private final ItemSourceRegistry registry;
    private final Consumer<Player> recount;
    private final Supplier<? extends Collection<? extends Player>> onlinePlayers;
    private final MainThread mainThread;

    ItemSourcesImpl(ItemSourceRegistry registry, Consumer<Player> recount,
                    Supplier<? extends Collection<? extends Player>> onlinePlayers, MainThread mainThread) {
        this.registry = registry;
        this.recount = recount;
        this.onlinePlayers = onlinePlayers;
        this.mainThread = mainThread;
    }

    @Override
    public @NotNull ItemSourceRegistration register(@NotNull Plugin owner, @NotNull String id,
                                                    @NotNull ItemSource source) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        mainThread.check();
        if (!owner.isEnabled()) {
            throw new IllegalStateException(owner.getName() + " is not enabled: register item sources in onEnable");
        }
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Item source id '" + id
                    + "' must be lowercase letters, digits, '_' and '-'");
        }
        ItemSourceRegistry.Entry entry = registry.add(owner, id, source).orElseThrow(() ->
                new IllegalArgumentException(owner.getName() + " already has an item source '" + id + "'"));
        recountOnline();
        return new Registration(entry);
    }

    @Override
    public void recount(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        mainThread.check();
        recount.accept(player);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (registry.removeOwner(event.getPlugin())) {
            recountOnline();
        }
    }

    private void recountOnline() {
        List.copyOf(onlinePlayers.get()).forEach(recount);
    }

    private final class Registration implements ItemSourceRegistration {
        private final ItemSourceRegistry.Entry entry;

        private Registration(ItemSourceRegistry.Entry entry) {
            this.entry = entry;
        }

        @Override
        public @NotNull Plugin owner() {
            return entry.owner();
        }

        @Override
        public @NotNull String id() {
            return entry.id();
        }

        @Override
        public boolean isRegistered() {
            return entry.isRegistered();
        }

        @Override
        public void unregister() {
            mainThread.check();
            if (registry.remove(entry)) {
                recountOnline();
            }
        }
    }
}
