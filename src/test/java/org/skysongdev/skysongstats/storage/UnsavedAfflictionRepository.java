package org.skysongdev.skysongstats.storage;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

// For tests that have no MySQL, like UnsavedSkillRepository: injuries and
// conditions live in memory in AfflictionService, and every change is reported
// as not saved.
public final class UnsavedAfflictionRepository implements AfflictionRepository {
    private final Consumer<String> log;

    public UnsavedAfflictionRepository(Consumer<String> log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void createTables() {
        // Nothing to create: there is no database.
    }

    @Override
    public Map<ProfileKey, StoredAfflictions> loadAll() {
        return Map.of();
    }

    @Override
    public void saveInjury(ProfileKey character, String injuryId, long awardedAt) {
        notSaved(character, "injury " + injuryId);
    }

    @Override
    public void deleteInjury(ProfileKey character, String injuryId) {
        notSaved(character, "removing injury " + injuryId);
    }

    @Override
    public void saveCondition(ProfileKey character, String conditionId, StoredCondition condition) {
        notSaved(character, "condition " + conditionId + " level " + condition.level());
    }

    @Override
    public void deleteCondition(ProfileKey character, String conditionId) {
        notSaved(character, "removing condition " + conditionId);
    }

    @Override
    public void saveKnownInjury(ProfileKey character, String injuryId, StoredKnownInjury known) {
        notSaved(character, "knowing injury " + injuryId);
    }

    @Override
    public void deleteKnownInjury(ProfileKey character, String injuryId) {
        notSaved(character, "forgetting injury " + injuryId);
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        notSaved(character, "deleting the character's injuries and conditions");
    }

    private void notSaved(ProfileKey character, String change) {
        log.accept("Not saved, the injury tables are unavailable: " + character + " " + change);
    }
}
