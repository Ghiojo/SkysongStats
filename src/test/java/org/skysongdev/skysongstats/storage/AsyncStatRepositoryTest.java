package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that {@link AsyncStatRepository} sends writes to the background writer,
 * in order, and keeps startup calls on the calling thread.
 */
class AsyncStatRepositoryTest {

    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "main");

    // Records each call with the thread it ran on.
    private static final class RecordingRepository implements StatRepository {
        final List<String> calls = new CopyOnWriteArrayList<>();

        private void record(String call) {
            calls.add(call + " @ " + Thread.currentThread().getName());
        }

        @Override
        public void createTables() {
            record("createTables");
        }

        @Override
        public Map<ProfileKey, StoredStats> loadAll() {
            record("loadAll");
            return Map.of();
        }

        @Override
        public void saveValue(ProfileKey character, String statId, int value) {
            record("saveValue " + statId + "=" + value);
        }

        @Override
        public void saveModifier(ProfileKey character, StoredModifier modifier) {
            record("saveModifier " + modifier.id());
        }

        @Override
        public void deleteModifier(String modifierId) {
            record("deleteModifier " + modifierId);
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
        AsyncStatRepository repository = new AsyncStatRepository(recording, writer);
        String caller = Thread.currentThread().getName();

        repository.createTables();
        repository.loadAll();
        repository.saveValue(CHARACTER, "hp", 50);
        repository.saveModifier(CHARACTER, new StoredModifier("m1", "con", 1));
        repository.deleteModifier("m1");
        repository.deleteCharacter(CHARACTER);
        assertTrue(writer.close(Duration.ofSeconds(10)));

        String background = "SkysongStats database writer";
        assertEquals(List.of(
                "createTables @ " + caller,
                "loadAll @ " + caller,
                "saveValue hp=50 @ " + background,
                "saveModifier m1 @ " + background,
                "deleteModifier m1 @ " + background,
                "deleteCharacter main @ " + background), recording.calls);
    }
}
