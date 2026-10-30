package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that {@link AsyncSkillRepository} sends writes to the background writer,
 * in order, and keeps startup calls on the calling thread.
 */
class AsyncSkillRepositoryTest {

    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "main");

    // Records each call with the thread it ran on.
    private static final class RecordingRepository implements SkillRepository {
        final List<String> calls = new CopyOnWriteArrayList<>();

        private void record(String call) {
            calls.add(call + " @ " + Thread.currentThread().getName());
        }

        @Override
        public void createTables() {
            record("createTables");
        }

        @Override
        public Map<ProfileKey, Map<String, Integer>> loadAll() {
            record("loadAll");
            return Map.of();
        }

        @Override
        public void saveXp(ProfileKey character, String skillId, int xp) {
            record("saveXp " + skillId + "=" + xp);
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
        AsyncSkillRepository repository = new AsyncSkillRepository(recording, writer);
        String caller = Thread.currentThread().getName();

        repository.createTables();
        repository.loadAll();
        repository.saveXp(CHARACTER, "farming", 16);
        repository.deleteCharacter(CHARACTER);
        assertTrue(writer.close(Duration.ofSeconds(10)));

        String background = "SkysongStats database writer";
        assertEquals(List.of(
                "createTables @ " + caller,
                "loadAll @ " + caller,
                "saveXp farming=16 @ " + background,
                "deleteCharacter main @ " + background), recording.calls);
    }
}
