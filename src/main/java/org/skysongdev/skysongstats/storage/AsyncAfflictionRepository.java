package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;

// Wraps an AfflictionRepository so its writes run on the AsyncWriter, in order
// with the stat, skill and proficiency writes. Creating the tables and loading
// happen once at startup and stay on the calling thread.
public class AsyncAfflictionRepository implements AfflictionRepository {
    private final AfflictionRepository repository;
    private final AsyncWriter writer;

    public AsyncAfflictionRepository(AfflictionRepository repository, AsyncWriter writer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public void createTables() {
        repository.createTables();
    }

    @Override
    public Map<ProfileKey, StoredAfflictions> loadAll() {
        return repository.loadAll();
    }

    @Override
    public void saveInjury(ProfileKey character, String injuryId, long awardedAt) {
        writer.submit(() -> repository.saveInjury(character, injuryId, awardedAt));
    }

    @Override
    public void deleteInjury(ProfileKey character, String injuryId) {
        writer.submit(() -> repository.deleteInjury(character, injuryId));
    }

    @Override
    public void saveCondition(ProfileKey character, String conditionId, StoredCondition condition) {
        writer.submit(() -> repository.saveCondition(character, conditionId, condition));
    }

    @Override
    public void deleteCondition(ProfileKey character, String conditionId) {
        writer.submit(() -> repository.deleteCondition(character, conditionId));
    }

    @Override
    public void saveKnownInjury(ProfileKey character, String injuryId, StoredKnownInjury known) {
        writer.submit(() -> repository.saveKnownInjury(character, injuryId, known));
    }

    @Override
    public void deleteKnownInjury(ProfileKey character, String injuryId) {
        writer.submit(() -> repository.deleteKnownInjury(character, injuryId));
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        writer.submit(() -> repository.deleteCharacter(character));
    }
}
