package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The repository used when the stat tables can't be opened.
 */
class UnsavedStatRepositoryTest {
    private final List<String> log = new ArrayList<>();
    private final UnsavedStatRepository repository = new UnsavedStatRepository(log::add);
    private final ProfileKey alice = new ProfileKey(UUID.randomUUID(), "Default");

    @Test
    void loadsNothing() {
        repository.createTables();
        assertTrue(repository.loadAll().isEmpty());
        assertTrue(log.isEmpty());
    }

    @Test
    void reportsEveryChangeAsNotSaved() {
        repository.saveValue(alice, "str", 2);
        repository.saveModifier(alice, new StoredModifier("m1", "str", 1));
        repository.deleteModifier("m1");
        repository.deleteCharacter(alice);
        assertEquals(4, log.size());
        assertTrue(log.stream().allMatch(line -> line.startsWith("Not saved")), log.toString());
        assertTrue(log.get(0).contains("str = 2"), log.get(0));
    }
}
