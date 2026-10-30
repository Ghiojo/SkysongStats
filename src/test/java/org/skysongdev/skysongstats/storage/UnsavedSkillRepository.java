package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

// For tests that have no MySQL, like UnsavedStatRepository: skill XP lives in
// memory, and every change is reported as not saved.
public final class UnsavedSkillRepository implements SkillRepository {
    private final Consumer<String> log;

    public UnsavedSkillRepository(Consumer<String> log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void createTables() {
        // Nothing to create: there is no database.
    }

    @Override
    public Map<ProfileKey, Map<String, Integer>> loadAll() {
        return Map.of();
    }

    @Override
    public void saveXp(ProfileKey character, String skillId, int xp) {
        notSaved(character, skillId + " = " + xp + " XP");
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        notSaved(character, "deleting the character's skills");
    }

    private void notSaved(ProfileKey character, String change) {
        log.accept("Not saved, the skill table is unavailable: " + character + " " + change);
    }
}
