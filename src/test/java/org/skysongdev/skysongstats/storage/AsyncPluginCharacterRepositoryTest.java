package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that {@link AsyncPluginCharacterRepository} sends writes to the background
 * writer, in order, and keeps startup calls on the calling thread.
 */
class AsyncPluginCharacterRepositoryTest {

    private static final ProfileKey GUARD = new ProfileKey(UUID.randomUUID(), "npc");

    // Records each call with the thread it ran on.
    private static final class RecordingRepository implements PluginCharacterRepository {
        final List<String> calls = new CopyOnWriteArrayList<>();

        private void record(String call) {
            calls.add(call + " @ " + Thread.currentThread().getName());
        }

        @Override
        public void createTables() {
            record("createTables");
        }

        @Override
        public Map<ProfileKey, String> loadAll() {
            record("loadAll");
            return Map.of();
        }

        @Override
        public void save(ProfileKey character, String owner) {
            record("save " + character.profile() + " " + owner);
        }

        @Override
        public void delete(ProfileKey character) {
            record("delete " + character.profile());
        }
    }

    @Test
    void writesRunInOrderOnTheWriterAndStartupCallsDoNot() {
        RecordingRepository recording = new RecordingRepository();
        AsyncWriter writer = new AsyncWriter(e -> fail(e));
        AsyncPluginCharacterRepository repository = new AsyncPluginCharacterRepository(recording, writer);
        String caller = Thread.currentThread().getName();

        repository.createTables();
        repository.loadAll();
        repository.save(GUARD, "SkysongNPCs");
        repository.delete(GUARD);
        assertTrue(writer.close(Duration.ofSeconds(10)));

        String background = "SkysongStats database writer";
        assertEquals(List.of(
                "createTables @ " + caller,
                "loadAll @ " + caller,
                "save npc SkysongNPCs @ " + background,
                "delete npc @ " + background), recording.calls);
    }
}
