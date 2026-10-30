package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;

// Wraps a ProficiencyRepository so its writes run on the AsyncWriter, after any
// stat and skill writes queued before them. Creating the tables and loading
// happen once at startup and stay on the calling thread.
public class AsyncProficiencyRepository implements ProficiencyRepository {
    private final ProficiencyRepository repository;
    private final AsyncWriter writer;

    public AsyncProficiencyRepository(ProficiencyRepository repository, AsyncWriter writer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public void createTables() {
        repository.createTables();
    }

    @Override
    public Map<ProfileKey, StoredProficiencies> loadAll() {
        return repository.loadAll();
    }

    @Override
    public void saveTier(ProfileKey character, String proficiencyId, String tierId) {
        writer.submit(() -> repository.saveTier(character, proficiencyId, tierId));
    }

    @Override
    public void saveFocus(ProfileKey character, StoredFocus focus) {
        writer.submit(() -> repository.saveFocus(character, focus));
    }

    @Override
    public void clearFocus(ProfileKey character) {
        writer.submit(() -> repository.clearFocus(character));
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        writer.submit(() -> repository.deleteCharacter(character));
    }
}
