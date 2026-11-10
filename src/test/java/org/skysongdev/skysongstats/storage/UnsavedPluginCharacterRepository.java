package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

// For tests that have no MySQL, like UnsavedStatRepository: there are no saved
// plugin characters, and every change is reported as not saved.
public final class UnsavedPluginCharacterRepository implements PluginCharacterRepository {
    private final Consumer<String> log;

    public UnsavedPluginCharacterRepository(Consumer<String> log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void createTables() {
        // Nothing to create: there is no database.
    }

    @Override
    public Map<ProfileKey, String> loadAll() {
        return Map.of();
    }

    @Override
    public void save(ProfileKey character, String owner) {
        log.accept("Not saved, the plugin character table is unavailable: " + character + " owned by " + owner);
    }

    @Override
    public void delete(ProfileKey character) {
        log.accept("Not saved, the plugin character table is unavailable: deleting " + character);
    }
}
