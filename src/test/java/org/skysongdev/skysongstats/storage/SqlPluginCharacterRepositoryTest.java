package org.skysongdev.skysongstats.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs {@link SqlPluginCharacterRepository}'s SQL against an in-memory H2 database in
 * MySQL mode. Each test gets its own empty database.
 */
class SqlPluginCharacterRepositoryTest {

    private static final String NPCS = "SkysongNPCs";
    private static final String SHOPS = "Shops";
    private static final ProfileKey GUARD = new ProfileKey(UUID.randomUUID(), "npc");
    private static final ProfileKey MERCHANT = new ProfileKey(UUID.randomUUID(), "npc");

    private SqlPluginCharacterRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        repository = new SqlPluginCharacterRepository(dataSource);
        repository.createTables();
    }

    @Test
    void startsEmpty() {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void createTablesCanRunAgain() {
        repository.save(GUARD, NPCS);

        assertDoesNotThrow(repository::createTables);
        assertEquals(Map.of(GUARD, NPCS), repository.loadAll());
    }

    @Test
    void savesEachCharacterWithItsOwner() {
        repository.save(GUARD, NPCS);
        repository.save(MERCHANT, SHOPS);

        assertEquals(Map.of(GUARD, NPCS, MERCHANT, SHOPS), repository.loadAll());
    }

    @Test
    void savingAgainReplacesTheOwner() {
        repository.save(GUARD, NPCS);
        repository.save(GUARD, SHOPS);

        assertEquals(Map.of(GUARD, SHOPS), repository.loadAll());
    }

    @Test
    void deletingACharacterKeepsTheOthers() {
        repository.save(GUARD, NPCS);
        repository.save(MERCHANT, NPCS);

        repository.delete(GUARD);

        assertEquals(Map.of(MERCHANT, NPCS), repository.loadAll());
    }
}
