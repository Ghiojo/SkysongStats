package org.skysongdev.skysongstats.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs {@link SqlProficiencyRepository}'s SQL against an in-memory H2 database in
 * MySQL mode. Each test gets its own empty database.
 */
class SqlProficiencyRepositoryTest {

    private static final ProfileKey ALICE_MAIN = new ProfileKey(UUID.randomUUID(), "main");
    private static final ProfileKey ALICE_ALT = new ProfileKey(ALICE_MAIN.player(), "alt");
    private static final ProfileKey BOB_MAIN = new ProfileKey(UUID.randomUUID(), "main");

    private SqlProficiencyRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        repository = new SqlProficiencyRepository(dataSource);
        repository.createTables();
    }

    @Test
    void startsEmpty() {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void createTablesCanRunAgain() {
        repository.saveTier(ALICE_MAIN, "axes", "adept");
        assertDoesNotThrow(repository::createTables);
        assertEquals(Map.of("axes", "adept"), repository.loadAll().get(ALICE_MAIN).tiers());
    }

    @Test
    void savesAndReplacesTiers() {
        repository.saveTier(ALICE_MAIN, "axes", "untrained");
        repository.saveTier(ALICE_MAIN, "bows", "proficient");
        repository.saveTier(ALICE_MAIN, "axes", "master");
        StoredProficiencies saved = repository.loadAll().get(ALICE_MAIN);
        assertEquals(Map.of("axes", "master", "bows", "proficient"), saved.tiers());
        assertTrue(saved.focusOptions().isEmpty());
    }

    @Test
    void savesReplacesAndClearsTheFocus() {
        repository.saveTier(ALICE_MAIN, "axes", "proficient");
        repository.saveFocus(ALICE_MAIN, new StoredFocus("axes", 2));
        assertEquals(new StoredFocus("axes", 2), repository.loadAll().get(ALICE_MAIN).focus());

        repository.saveFocus(ALICE_MAIN, new StoredFocus("bows", 0));
        assertEquals(new StoredFocus("bows", 0), repository.loadAll().get(ALICE_MAIN).focus());

        repository.clearFocus(ALICE_MAIN);
        StoredProficiencies saved = repository.loadAll().get(ALICE_MAIN);
        assertNull(saved.focus());
        assertEquals(Map.of("axes", "proficient"), saved.tiers());
    }

    @Test
    void aCharacterWithOnlyAFocusIsLoaded() {
        repository.saveFocus(BOB_MAIN, new StoredFocus("axes", 1));
        StoredProficiencies saved = repository.loadAll().get(BOB_MAIN);
        assertTrue(saved.tiers().isEmpty());
        assertEquals(new StoredFocus("axes", 1), saved.focus());
    }

    @Test
    void keepsCharactersAndProfilesApart() {
        repository.saveTier(ALICE_MAIN, "axes", "proficient");
        repository.saveTier(ALICE_ALT, "axes", "adept");
        repository.saveTier(BOB_MAIN, "axes", "master");
        repository.saveFocus(ALICE_ALT, new StoredFocus("bows", 3));
        Map<ProfileKey, StoredProficiencies> all = repository.loadAll();
        assertEquals("proficient", all.get(ALICE_MAIN).tiers().get("axes"));
        assertEquals("adept", all.get(ALICE_ALT).tiers().get("axes"));
        assertEquals("master", all.get(BOB_MAIN).tiers().get("axes"));
        assertNull(all.get(ALICE_MAIN).focus());
        assertEquals(new StoredFocus("bows", 3), all.get(ALICE_ALT).focus());
    }

    @Test
    void deletingACharacterRemovesItsTiersAndFocusAndKeepsTheOthers() {
        repository.saveTier(ALICE_MAIN, "axes", "proficient");
        repository.saveFocus(ALICE_MAIN, new StoredFocus("axes", 2));
        repository.saveTier(ALICE_ALT, "axes", "adept");
        repository.saveFocus(ALICE_ALT, new StoredFocus("bows", 1));
        repository.deleteCharacter(ALICE_MAIN);
        Map<ProfileKey, StoredProficiencies> all = repository.loadAll();
        assertFalse(all.containsKey(ALICE_MAIN));
        assertEquals(new StoredProficiencies(Map.of("axes", "adept"), new StoredFocus("bows", 1)), all.get(ALICE_ALT));
    }

    @Test
    void aBrokenDatabaseIsReportedAsAStorageException() {
        JdbcDataSource broken = new JdbcDataSource();
        broken.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;IFEXISTS=TRUE");
        SqlProficiencyRepository missing = new SqlProficiencyRepository(broken);
        assertThrows(StorageException.class, missing::loadAll);
        assertThrows(StorageException.class, () -> missing.saveTier(ALICE_MAIN, "axes", "adept"));
        assertThrows(StorageException.class, () -> missing.saveFocus(ALICE_MAIN, new StoredFocus("axes", 1)));
        assertThrows(StorageException.class, () -> missing.clearFocus(ALICE_MAIN));
        assertThrows(StorageException.class, () -> missing.deleteCharacter(ALICE_MAIN));
        assertThrows(StorageException.class, missing::createTables);
    }
}
