package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;

// Wraps a PluginCharacterRepository so its writes run on the AsyncWriter, after
// any stat writes queued before them. Creating the table and loading happen once
// at startup and stay on the calling thread.
public class AsyncPluginCharacterRepository implements PluginCharacterRepository {
    private final PluginCharacterRepository repository;
    private final AsyncWriter writer;

    public AsyncPluginCharacterRepository(PluginCharacterRepository repository, AsyncWriter writer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public void createTables() {
        repository.createTables();
    }

    @Override
    public Map<ProfileKey, String> loadAll() {
        return repository.loadAll();
    }

    @Override
    public void save(ProfileKey character, String owner) {
        writer.submit(() -> repository.save(character, owner));
    }

    @Override
    public void delete(ProfileKey character) {
        writer.submit(() -> repository.delete(character));
    }
}
