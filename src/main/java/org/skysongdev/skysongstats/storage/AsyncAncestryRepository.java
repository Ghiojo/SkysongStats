package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;

// Wraps an AncestryRepository so its writes run on the AsyncWriter, after any
// stat writes queued before them. Creating the tables and loading happen once at
// startup and stay on the calling thread.
public class AsyncAncestryRepository implements AncestryRepository {
    private final AncestryRepository repository;
    private final AsyncWriter writer;

    public AsyncAncestryRepository(AncestryRepository repository, AsyncWriter writer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public void createTables() {
        repository.createTables();
    }

    @Override
    public Map<ProfileKey, StoredAncestry> loadAll() {
        return repository.loadAll();
    }

    @Override
    public void save(ProfileKey character, StoredAncestry ancestry) {
        writer.submit(() -> repository.save(character, ancestry));
    }

    @Override
    public void delete(ProfileKey character) {
        writer.submit(() -> repository.delete(character));
    }
}
