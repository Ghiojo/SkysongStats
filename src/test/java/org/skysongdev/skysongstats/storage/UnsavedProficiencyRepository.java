package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

// For tests that have no MySQL, like UnsavedSkillRepository: proficiencies live
// in memory, and every change is reported as not saved.
public final class UnsavedProficiencyRepository implements ProficiencyRepository {
    private final Consumer<String> log;

    public UnsavedProficiencyRepository(Consumer<String> log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void createTables() {
        // Nothing to create: there is no database.
    }

    @Override
    public Map<ProfileKey, StoredProficiencies> loadAll() {
        return Map.of();
    }

    @Override
    public void saveTier(ProfileKey character, String proficiencyId, String tierId) {
        notSaved(character, proficiencyId + " = " + tierId);
    }

    @Override
    public void saveFocus(ProfileKey character, StoredFocus focus) {
        notSaved(character, "focus " + focus.proficiencyId() + " with " + focus.points() + " points");
    }

    @Override
    public void clearFocus(ProfileKey character) {
        notSaved(character, "clearing the focus");
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        notSaved(character, "deleting the character's proficiencies");
    }

    private void notSaved(ProfileKey character, String change) {
        log.accept("Not saved, the proficiency tables are unavailable: " + character + " " + change);
    }
}
