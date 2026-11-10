package org.skysongdev.skysongstats.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs {@link SqlAncestryRepository}'s SQL against an in-memory H2 database in MySQL
 * mode. Each test gets its own empty database.
 */
class SqlAncestryRepositoryTest {

    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "Default");
    private static final ProfileKey BOB = new ProfileKey(UUID.randomUUID(), "Second");
    private static final StoredAncestry DWARF = new StoredAncestry("dwarf", false, List.of(
            new StoredAncestryAdjustment("con", 1, StoredAncestryAdjustment.FIXED),
            new StoredAncestryAdjustment("spd", -1, StoredAncestryAdjustment.FIXED),
            new StoredAncestryAdjustment("str", 1, StoredAncestryAdjustment.choice(1))));
    private static final StoredAncestry HUMAN = new StoredAncestry("human", true, List.of());

    private SqlAncestryRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        repository = new SqlAncestryRepository(dataSource);
        repository.createTables();
    }

    @Test
    void startsEmpty() {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void createTablesCanRunAgain() {
        repository.save(ALICE, DWARF);

        assertDoesNotThrow(repository::createTables);
        assertEquals(Map.of(ALICE, DWARF), repository.loadAll());
    }

    @Test
    void savesEachCharacterWithItsAdjustmentsInOrder() {
        repository.save(ALICE, DWARF);
        repository.save(BOB, HUMAN);

        assertEquals(Map.of(ALICE, DWARF, BOB, HUMAN), repository.loadAll());
    }

    @Test
    void savingAgainReplacesEverything() {
        repository.save(ALICE, DWARF);
        StoredAncestry rechosen = new StoredAncestry("dwarf", true, DWARF.adjustments().subList(0, 2));
        repository.save(ALICE, rechosen);

        assertEquals(Map.of(ALICE, rechosen), repository.loadAll());
    }

    @Test
    void deleteForgetsOnlyThatCharacter() {
        repository.save(ALICE, DWARF);
        repository.save(BOB, HUMAN);
        repository.delete(ALICE);

        assertEquals(Map.of(BOB, HUMAN), repository.loadAll());
    }

    @Test
    void deletingACharacterWithoutAnAncestryDoesNothing() {
        assertDoesNotThrow(() -> repository.delete(ALICE));
        assertTrue(repository.loadAll().isEmpty());
    }
}
