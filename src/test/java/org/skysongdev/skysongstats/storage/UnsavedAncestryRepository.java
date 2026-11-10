package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

// For tests that have no MySQL, like UnsavedStatRepository: no character has a
// saved ancestry, and every change is reported as not saved.
public final class UnsavedAncestryRepository implements AncestryRepository {
    private final Consumer<String> log;

    public UnsavedAncestryRepository(Consumer<String> log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void createTables() {
        // Nothing to create: there is no database.
    }

    @Override
    public Map<ProfileKey, StoredAncestry> loadAll() {
        return Map.of();
    }

    @Override
    public void save(ProfileKey character, StoredAncestry ancestry) {
        log.accept("Not saved, the ancestry tables are unavailable: " + character + " " + ancestry);
    }

    @Override
    public void delete(ProfileKey character) {
        log.accept("Not saved, the ancestry tables are unavailable: deleting the ancestry of " + character);
    }
}
