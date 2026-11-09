package org.skysongdev.skysongstats.messages;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// The plugins that say injury award messages themselves (the API's
// announceAwards). While there is one, SkysongStats doesn't send them to the
// injured player. A plugin's entries go when it disables.
public final class AwardAnnouncers {
    private final List<Entry> entries = new ArrayList<>();

    // One registration. Stays unregistered once removed.
    public static final class Entry {
        private final Plugin owner;
        private boolean registered = true;

        private Entry(Plugin owner) {
            this.owner = owner;
        }

        public Plugin owner() {
            return owner;
        }

        public boolean isRegistered() {
            return registered && owner.isEnabled();
        }
    }

    public Entry add(Plugin owner) {
        Entry entry = new Entry(Objects.requireNonNull(owner, "owner"));
        entries.add(entry);
        return entry;
    }

    // False when it was removed already.
    public boolean remove(Entry entry) {
        entry.registered = false;
        return entries.remove(entry);
    }

    // Removes every entry of a plugin; true when it had any.
    public boolean removeOwner(Plugin owner) {
        List<Entry> owned = entries.stream().filter(entry -> entry.owner.equals(owner)).toList();
        owned.forEach(this::remove);
        return !owned.isEmpty();
    }

    // Whether another plugin announces awards now.
    public boolean anyone() {
        return entries.stream().anyMatch(Entry::isRegistered);
    }
}
