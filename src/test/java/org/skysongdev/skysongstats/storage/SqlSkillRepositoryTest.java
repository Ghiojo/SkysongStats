package org.skysongdev.skysongstats.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs {@link SqlSkillRepository}'s SQL against an in-memory H2 database in MySQL
 * mode. Each test gets its own empty database.
 */
class SqlSkillRepositoryTest {

    private static final ProfileKey ALICE_MAIN = new ProfileKey(UUID.randomUUID(), "main");
    private static final ProfileKey ALICE_ALT = new ProfileKey(ALICE_MAIN.player(), "alt");
    private static final ProfileKey BOB_MAIN = new ProfileKey(UUID.randomUUID(), "main");

    private SqlSkillRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        repository = new SqlSkillRepository(dataSource);
        repository.createTables();
    }

    @Test
    void startsEmpty() {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void createTablesCanRunAgain() {
        repository.saveXp(ALICE_MAIN, "farming", 16);
        assertDoesNotThrow(repository::createTables);
        assertEquals(Map.of("farming", 16), repository.loadAll().get(ALICE_MAIN));
    }

    @Test
    void savesAndReplacesXp() {
        repository.saveXp(ALICE_MAIN, "farming", 16);
        repository.saveXp(ALICE_MAIN, "tande", 40);
        repository.saveXp(ALICE_MAIN, "farming", 20);
        assertEquals(Map.of("farming", 20, "tande", 40), repository.loadAll().get(ALICE_MAIN));
    }

    @Test
    void keepsCharactersAndProfilesApart() {
        repository.saveXp(ALICE_MAIN, "farming", 1);
        repository.saveXp(ALICE_ALT, "farming", 2);
        repository.saveXp(BOB_MAIN, "farming", 3);
        Map<ProfileKey, Map<String, Integer>> all = repository.loadAll();
        assertEquals(1, all.get(ALICE_MAIN).get("farming"));
        assertEquals(2, all.get(ALICE_ALT).get("farming"));
        assertEquals(3, all.get(BOB_MAIN).get("farming"));
    }

    @Test
    void deletingACharacterKeepsTheOthers() {
        repository.saveXp(ALICE_MAIN, "farming", 1);
        repository.saveXp(ALICE_ALT, "farming", 2);
        repository.deleteCharacter(ALICE_MAIN);
        Map<ProfileKey, Map<String, Integer>> all = repository.loadAll();
        assertFalse(all.containsKey(ALICE_MAIN));
        assertEquals(Map.of("farming", 2), all.get(ALICE_ALT));
    }

    @Test
    void aBrokenDatabaseIsReportedAsAStorageException() {
        JdbcDataSource broken = new JdbcDataSource();
        broken.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;IFEXISTS=TRUE");
        SqlSkillRepository missing = new SqlSkillRepository(broken);
        assertThrows(StorageException.class, missing::loadAll);
        assertThrows(StorageException.class, () -> missing.saveXp(ALICE_MAIN, "farming", 1));
        assertThrows(StorageException.class, () -> missing.deleteCharacter(ALICE_MAIN));
        assertThrows(StorageException.class, missing::createTables);
    }
}
