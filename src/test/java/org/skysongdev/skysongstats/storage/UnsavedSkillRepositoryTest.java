package org.skysongdev.skysongstats.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The skill repository used when the database can't be opened.
 */
class UnsavedSkillRepositoryTest {
    private final List<String> log = new ArrayList<>();
    private final UnsavedSkillRepository repository = new UnsavedSkillRepository(log::add);
    private final ProfileKey alice = new ProfileKey(UUID.randomUUID(), "Default");

    @Test
    void loadsNothing() {
        repository.createTables();
        assertTrue(repository.loadAll().isEmpty());
        assertTrue(log.isEmpty());
    }

    @Test
    void reportsEveryChangeAsNotSaved() {
        repository.saveXp(alice, "farming", 16);
        repository.deleteCharacter(alice);
        assertEquals(2, log.size());
        assertTrue(log.stream().allMatch(line -> line.startsWith("Not saved")), log.toString());
        assertTrue(log.get(0).contains("farming = 16 XP"), log.get(0));
    }
}
