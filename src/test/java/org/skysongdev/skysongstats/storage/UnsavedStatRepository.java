package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

// For tests that have no MySQL. Stats live in memory: nothing is loaded, and
// every change is reported as not saved.
public final class UnsavedStatRepository implements StatRepository {
    private final Consumer<String> log;

    public UnsavedStatRepository(Consumer<String> log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void createTables() {
        // Nothing to create: there is no database.
    }

    @Override
    public Map<ProfileKey, StoredStats> loadAll() {
        return Map.of();
    }

    @Override
    public void saveValue(ProfileKey character, String statId, int value) {
        notSaved(character, statId + " = " + value);
    }

    @Override
    public void saveModifier(ProfileKey character, StoredModifier modifier) {
        notSaved(character, "modifier " + modifier.target() + " " + modifier.amount());
    }

    @Override
    public void deleteModifier(String modifierId) {
        log.accept("Not saved, the stat tables are unavailable: removing modifier " + modifierId);
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        notSaved(character, "deleting the character");
    }

    private void notSaved(ProfileKey character, String change) {
        log.accept("Not saved, the stat tables are unavailable: " + character + " " + change);
    }
}
