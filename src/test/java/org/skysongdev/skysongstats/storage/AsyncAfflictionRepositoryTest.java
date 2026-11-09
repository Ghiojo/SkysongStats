package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that {@link AsyncAfflictionRepository} sends writes to the background
 * writer, in order, and keeps startup calls on the calling thread.
 */
class AsyncAfflictionRepositoryTest {

    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "main");

    // Records each call with the thread it ran on.
    private static final class RecordingRepository implements AfflictionRepository {
        final List<String> calls = new CopyOnWriteArrayList<>();

        private void record(String call) {
            calls.add(call + " @ " + Thread.currentThread().getName());
        }

        @Override
        public void createTables() {
            record("createTables");
        }

        @Override
        public Map<ProfileKey, StoredAfflictions> loadAll() {
            record("loadAll");
            return Map.of();
        }

        @Override
        public void saveInjury(ProfileKey character, String injuryId, long awardedAt) {
            record("saveInjury " + injuryId);
        }

        @Override
        public void deleteInjury(ProfileKey character, String injuryId) {
            record("deleteInjury " + injuryId);
        }

        @Override
        public void saveCondition(ProfileKey character, String conditionId, StoredCondition condition) {
            record("saveCondition " + conditionId + "=" + condition.level());
        }

        @Override
        public void deleteCondition(ProfileKey character, String conditionId) {
            record("deleteCondition " + conditionId);
        }

        @Override
        public void saveKnownInjury(ProfileKey character, String injuryId, StoredKnownInjury known) {
            record("saveKnownInjury " + injuryId + "=" + known.learntBy());
        }

        @Override
        public void deleteKnownInjury(ProfileKey character, String injuryId) {
            record("deleteKnownInjury " + injuryId);
        }

        @Override
        public void deleteCharacter(ProfileKey character) {
            record("deleteCharacter " + character.profile());
        }
    }

    @Test
    void writesRunInOrderOnTheWriterAndStartupCallsDoNot() {
        RecordingRepository recording = new RecordingRepository();
        AsyncWriter writer = new AsyncWriter(e -> fail(e));
        AsyncAfflictionRepository repository = new AsyncAfflictionRepository(recording, writer);
        String caller = Thread.currentThread().getName();

        repository.createTables();
        repository.loadAll();
        repository.saveInjury(CHARACTER, "cut", 1L);
        repository.deleteInjury(CHARACTER, "cut");
        repository.saveCondition(CHARACTER, "bleeding", new StoredCondition(2, 1L));
        repository.deleteCondition(CHARACTER, "bleeding");
        repository.saveKnownInjury(CHARACTER, "cut", new StoredKnownInjury(1L, LearnSource.AWARD));
        repository.deleteKnownInjury(CHARACTER, "cut");
        repository.deleteCharacter(CHARACTER);
        assertTrue(writer.close(Duration.ofSeconds(10)));

        String background = "SkysongStats database writer";
        assertEquals(List.of(
                "createTables @ " + caller,
                "loadAll @ " + caller,
                "saveInjury cut @ " + background,
                "deleteInjury cut @ " + background,
                "saveCondition bleeding=2 @ " + background,
                "deleteCondition bleeding @ " + background,
                "saveKnownInjury cut=AWARD @ " + background,
                "deleteKnownInjury cut @ " + background,
                "deleteCharacter main @ " + background), recording.calls);
    }
}
