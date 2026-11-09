package org.skysongdev.skysongstats.storage;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs {@link SqlAfflictionRepository}'s SQL against an in-memory H2 database in
 * MySQL mode. Each test gets its own empty database.
 */
class SqlAfflictionRepositoryTest {

    private static final ProfileKey ALICE_MAIN = new ProfileKey(UUID.randomUUID(), "main");
    private static final ProfileKey ALICE_ALT = new ProfileKey(ALICE_MAIN.player(), "alt");
    private static final ProfileKey BOB_MAIN = new ProfileKey(UUID.randomUUID(), "main");

    private JdbcDataSource dataSource;
    private SqlAfflictionRepository repository;

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        repository = new SqlAfflictionRepository(dataSource);
        repository.createTables();
    }

    private StoredAfflictions saved(ProfileKey character) {
        return repository.loadAll().get(character);
    }

    @Test
    void startsEmpty() {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void createTablesCanRunAgain() {
        repository.saveInjury(ALICE_MAIN, "cut", 10L);
        assertDoesNotThrow(repository::createTables);
        assertEquals(Map.of("cut", 10L), saved(ALICE_MAIN).injuries());
    }

    @Test
    void savesReplacesAndDeletesInjuries() {
        repository.saveInjury(ALICE_MAIN, "cut", 10L);
        repository.saveInjury(ALICE_MAIN, "broken-leg", 20L);
        repository.saveInjury(ALICE_MAIN, "cut", 30L);
        assertEquals(Map.of("cut", 30L, "broken-leg", 20L), saved(ALICE_MAIN).injuries());

        repository.deleteInjury(ALICE_MAIN, "cut");
        assertEquals(Map.of("broken-leg", 20L), saved(ALICE_MAIN).injuries());
    }

    @Test
    void savesReplacesAndDeletesConditions() {
        repository.saveCondition(ALICE_MAIN, "bleeding", new StoredCondition(1, 10L));
        repository.saveCondition(ALICE_MAIN, "bleeding", new StoredCondition(3, 20L));
        assertEquals(Map.of("bleeding", new StoredCondition(3, 20L)), saved(ALICE_MAIN).conditions());

        repository.deleteCondition(ALICE_MAIN, "bleeding");
        assertNull(saved(ALICE_MAIN));
    }

    @Test
    void savesReplacesAndDeletesKnownInjuries() {
        repository.saveKnownInjury(ALICE_MAIN, "cut", new StoredKnownInjury(10L, LearnSource.DISCOVER));
        repository.saveKnownInjury(ALICE_MAIN, "cut", new StoredKnownInjury(20L, LearnSource.AWARD));
        repository.saveKnownInjury(ALICE_MAIN, "burn", new StoredKnownInjury(30L, LearnSource.IMPORT));
        assertEquals(Map.of("cut", new StoredKnownInjury(20L, LearnSource.AWARD),
                "burn", new StoredKnownInjury(30L, LearnSource.IMPORT)), saved(ALICE_MAIN).known());

        repository.deleteKnownInjury(ALICE_MAIN, "cut");
        assertEquals(Map.of("burn", new StoredKnownInjury(30L, LearnSource.IMPORT)), saved(ALICE_MAIN).known());
    }

    @Test
    void aCharacterWithOnlyKnownInjuriesIsLoaded() {
        repository.saveKnownInjury(BOB_MAIN, "cut", new StoredKnownInjury(10L, LearnSource.API));
        StoredAfflictions bob = saved(BOB_MAIN);
        assertTrue(bob.injuries().isEmpty());
        assertTrue(bob.conditions().isEmpty());
        assertEquals(1, bob.known().size());
    }

    @Test
    void keepsCharactersAndProfilesApart() {
        repository.saveInjury(ALICE_MAIN, "cut", 10L);
        repository.saveInjury(ALICE_ALT, "burn", 20L);
        repository.saveCondition(BOB_MAIN, "bleeding", new StoredCondition(2, 30L));

        assertEquals(Map.of("cut", 10L), saved(ALICE_MAIN).injuries());
        assertEquals(Map.of("burn", 20L), saved(ALICE_ALT).injuries());
        assertEquals(Map.of("bleeding", new StoredCondition(2, 30L)), saved(BOB_MAIN).conditions());
    }

    @Test
    void deleteCharacterRemovesOnlyThatCharacter() {
        repository.saveInjury(ALICE_MAIN, "cut", 10L);
        repository.saveCondition(ALICE_MAIN, "bleeding", new StoredCondition(1, 10L));
        repository.saveKnownInjury(ALICE_MAIN, "cut", new StoredKnownInjury(10L, LearnSource.AWARD));
        repository.saveInjury(ALICE_ALT, "burn", 20L);

        repository.deleteCharacter(ALICE_MAIN);

        assertNull(saved(ALICE_MAIN));
        assertEquals(Map.of("burn", 20L), saved(ALICE_ALT).injuries());
    }

    @Test
    void anUnknownLearnSourceIsReadAsDiscover() throws SQLException {
        repository.saveKnownInjury(ALICE_MAIN, "cut", new StoredKnownInjury(10L, LearnSource.AWARD));
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE ss_known_injuries SET learnt_by = 'something-new'");
        }
        assertEquals(LearnSource.DISCOVER, saved(ALICE_MAIN).known().get("cut").learntBy());
    }
}
