package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;

// Wraps a StatRepository so its writes run on the AsyncWriter. Creating tables and
// loading happen once at startup and stay on the calling thread.
public class AsyncStatRepository implements StatRepository {
    private final StatRepository repository;
    private final AsyncWriter writer;

    public AsyncStatRepository(StatRepository repository, AsyncWriter writer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public void createTables() {
        repository.createTables();
    }

    @Override
    public Map<ProfileKey, StoredStats> loadAll() {
        return repository.loadAll();
    }

    @Override
    public void saveValue(ProfileKey character, String statId, int value) {
        writer.submit(() -> repository.saveValue(character, statId, value));
    }

    @Override
    public void saveModifier(ProfileKey character, StoredModifier modifier) {
        writer.submit(() -> repository.saveModifier(character, modifier));
    }

    @Override
    public void deleteModifier(String modifierId) {
        writer.submit(() -> repository.deleteModifier(modifierId));
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        writer.submit(() -> repository.deleteCharacter(character));
    }
}
