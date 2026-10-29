package org.skysongdev.skysongstats.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs {@link SqlStatRepository}'s SQL against an in-memory H2 database in MySQL
 * mode. Each test gets its own empty database.
 */
class SqlStatRepositoryTest {

    private static final ProfileKey ALICE_MAIN = new ProfileKey(UUID.randomUUID(), "main");
    private static final ProfileKey ALICE_ALT = new ProfileKey(ALICE_MAIN.player(), "alt");
    private static final ProfileKey BOB_MAIN = new ProfileKey(UUID.randomUUID(), "main");

    private SqlStatRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        repository = new SqlStatRepository(dataSource);
        repository.createTables();
    }

    private StoredStats stats(ProfileKey character) {
        return repository.loadAll().get(character);
    }

    @Test
    void createTablesCanRunAgain() {
        repository.saveValue(ALICE_MAIN, "str", 2);
        assertDoesNotThrow(repository::createTables);
        assertEquals(Map.of("str", 2), stats(ALICE_MAIN).values());
    }

    @Test
    void startsEmpty() {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void savesAndReplacesValues() {
        repository.saveValue(ALICE_MAIN, "str", 2);
        repository.saveValue(ALICE_MAIN, "hp", 70);
        repository.saveValue(ALICE_MAIN, "str", -1);
        assertEquals(Map.of("str", -1, "hp", 70), stats(ALICE_MAIN).values());
        assertEquals(List.of(), stats(ALICE_MAIN).modifiers());
    }

    @Test
    void keepsCharactersAndProfilesApart() {
        repository.saveValue(ALICE_MAIN, "str", 1);
        repository.saveValue(ALICE_ALT, "str", 2);
        repository.saveValue(BOB_MAIN, "str", 3);
        Map<ProfileKey, StoredStats> all = repository.loadAll();
        assertEquals(3, all.size());
        assertEquals(1, all.get(ALICE_MAIN).values().get("str"));
        assertEquals(2, all.get(ALICE_ALT).values().get("str"));
        assertEquals(3, all.get(BOB_MAIN).values().get("str"));
    }

    @Test
    void anyStatIdCanBeSavedWithoutChangingTheTables() {
        repository.saveValue(ALICE_MAIN, "a-stat-added-later", 5);
        assertEquals(5, stats(ALICE_MAIN).values().get("a-stat-added-later"));
    }

    @Test
    void modifiersLoadInTheOrderTheyWereAdded() {
        StoredModifier first = new StoredModifier("m1", "con", 1);
        StoredModifier second = new StoredModifier("m2", "max_potion", 2);
        StoredModifier third = new StoredModifier("m0", "str", -1);
        repository.saveModifier(ALICE_MAIN, first);
        repository.saveModifier(ALICE_MAIN, second);
        repository.saveModifier(ALICE_MAIN, third);
        assertEquals(List.of(first, second, third), stats(ALICE_MAIN).modifiers());
        assertTrue(stats(ALICE_MAIN).values().isEmpty(), "a character can have only modifiers");
    }

    @Test
    void savingAModifierAgainUpdatesItInPlace() {
        repository.saveModifier(ALICE_MAIN, new StoredModifier("m1", "con", 1));
        repository.saveModifier(ALICE_MAIN, new StoredModifier("m2", "str", 1));
        repository.saveModifier(ALICE_MAIN, new StoredModifier("m1", "con", 3));
        assertEquals(List.of(new StoredModifier("m1", "con", 3), new StoredModifier("m2", "str", 1)),
                stats(ALICE_MAIN).modifiers());
    }

    // Bug 4: deleting a modifier used to delete by list position and hit the wrong row.
    @Test
    void deletingAModifierRemovesOnlyThatOne() {
        repository.saveModifier(ALICE_MAIN, new StoredModifier("m1", "con", 1));
        repository.saveModifier(ALICE_MAIN, new StoredModifier("m2", "con", 1));
        repository.saveModifier(BOB_MAIN, new StoredModifier("m3", "con", 1));
        repository.deleteModifier("m1");
        assertEquals(List.of(new StoredModifier("m2", "con", 1)), stats(ALICE_MAIN).modifiers());
        assertEquals(1, stats(BOB_MAIN).modifiers().size());
        assertDoesNotThrow(() -> repository.deleteModifier("missing"));
    }

    // Bug 6: deleted profiles used to leave their modifiers behind.
    @Test
    void deletingACharacterRemovesItsValuesAndModifiersOnly() {
        repository.saveValue(ALICE_MAIN, "str", 1);
        repository.saveModifier(ALICE_MAIN, new StoredModifier("m1", "con", 1));
        repository.saveValue(ALICE_ALT, "str", 2);
        repository.saveModifier(ALICE_ALT, new StoredModifier("m2", "con", 1));
        repository.deleteCharacter(ALICE_MAIN);
        Map<ProfileKey, StoredStats> all = repository.loadAll();
        assertFalse(all.containsKey(ALICE_MAIN));
        assertEquals(Map.of("str", 2), all.get(ALICE_ALT).values());
        assertEquals(1, all.get(ALICE_ALT).modifiers().size());
    }

    @Test
    void loadedStatsCannotBeModified() {
        repository.saveValue(ALICE_MAIN, "str", 1);
        repository.saveModifier(ALICE_MAIN, new StoredModifier("m1", "con", 1));
        StoredStats stats = stats(ALICE_MAIN);
        assertThrows(UnsupportedOperationException.class, () -> stats.values().put("dex", 1));
        assertThrows(UnsupportedOperationException.class, () -> stats.modifiers().clear());
    }

    @Test
    void failuresAreReportedAsStorageExceptions() {
        JdbcDataSource empty = new JdbcDataSource();
        empty.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        SqlStatRepository withoutTables = new SqlStatRepository(empty);
        StorageException error = assertThrows(StorageException.class,
                () -> withoutTables.saveValue(ALICE_MAIN, "str", 1));
        assertTrue(error.getMessage().contains("str"));
        assertThrows(StorageException.class, withoutTables::loadAll);
        assertThrows(StorageException.class, () -> withoutTables.deleteCharacter(ALICE_MAIN));
    }
}
