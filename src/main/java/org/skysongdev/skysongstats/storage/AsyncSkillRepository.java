package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;

// Wraps a SkillRepository so its writes run on the AsyncWriter, after any stat
// writes queued before them. Creating the table and loading happen once at
// startup and stay on the calling thread.
public class AsyncSkillRepository implements SkillRepository {
    private final SkillRepository repository;
    private final AsyncWriter writer;

    public AsyncSkillRepository(SkillRepository repository, AsyncWriter writer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public void createTables() {
        repository.createTables();
    }

    @Override
    public Map<ProfileKey, Map<String, Integer>> loadAll() {
        return repository.loadAll();
    }

    @Override
    public void saveXp(ProfileKey character, String skillId, int xp) {
        writer.submit(() -> repository.saveXp(character, skillId, xp));
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        writer.submit(() -> repository.deleteCharacter(character));
    }
}
