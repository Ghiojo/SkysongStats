package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that {@link AsyncProficiencyRepository} sends writes to the background
 * writer, in order, and keeps startup calls on the calling thread.
 */
class AsyncProficiencyRepositoryTest {

    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "main");

    // Records each call with the thread it ran on.
    private static final class RecordingRepository implements ProficiencyRepository {
        final List<String> calls = new CopyOnWriteArrayList<>();

        private void record(String call) {
            calls.add(call + " @ " + Thread.currentThread().getName());
        }

        @Override
        public void createTables() {
            record("createTables");
        }

        @Override
        public Map<ProfileKey, StoredProficiencies> loadAll() {
            record("loadAll");
            return Map.of();
        }

        @Override
        public void saveTier(ProfileKey character, String proficiencyId, String tierId) {
            record("saveTier " + proficiencyId + "=" + tierId);
        }

        @Override
        public void saveFocus(ProfileKey character, StoredFocus focus) {
            record("saveFocus " + focus.proficiencyId() + "=" + focus.points());
        }

        @Override
        public void clearFocus(ProfileKey character) {
            record("clearFocus " + character.profile());
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
        AsyncProficiencyRepository repository = new AsyncProficiencyRepository(recording, writer);
        String caller = Thread.currentThread().getName();

        repository.createTables();
        repository.loadAll();
        repository.saveTier(CHARACTER, "axes", "adept");
        repository.saveFocus(CHARACTER, new StoredFocus("axes", 2));
        repository.clearFocus(CHARACTER);
        repository.deleteCharacter(CHARACTER);
        assertTrue(writer.close(Duration.ofSeconds(10)));

        String background = "SkysongStats database writer";
        assertEquals(List.of(
                "createTables @ " + caller,
                "loadAll @ " + caller,
                "saveTier axes=adept @ " + background,
                "saveFocus axes=2 @ " + background,
                "clearFocus main @ " + background,
                "deleteCharacter main @ " + background), recording.calls);
    }
}
