package org.skysongdev.skysongstats.service.downtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs {@link SqlDowntimeRepository} against the plugin's three downtime tables in a temporary SQLite database. */
class SqlDowntimeRepositoryTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private Connection connection;
    private SqlDowntimeRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        // The default data source opens a private temporary database that is deleted on close.
        connection = new SQLiteDataSource().getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE skysong_downtime(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), current_downtime INT)");
            statement.execute("CREATE TABLE skysong_character_downtime(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), profile varchar(20), current_downtime INT)");
            statement.execute("CREATE TABLE skysong_downtime_preferences(uuid varchar(36) primary key, isGlobal BOOLEAN)");
        }
        repository = new SqlDowntimeRepository(() -> connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void loadsRowsWrittenByTheOldCode() throws SQLException {
        execute("INSERT INTO skysong_downtime(uuid, current_downtime) VALUES (?, ?)", ALICE.toString(), 7);
        execute("INSERT INTO skysong_character_downtime(uuid, profile, current_downtime) VALUES (?, ?, ?)", ALICE.toString(), "Mage", 2);
        execute("INSERT INTO skysong_downtime_preferences(uuid, isGlobal) VALUES (?, ?)", ALICE.toString(), true);

        DowntimeSnapshot snapshot = repository.loadAll();

        assertEquals(Map.of(ALICE, 7), snapshot.globals());
        assertEquals(Map.of(new CharacterKey(ALICE, "Mage"), 2), snapshot.characters());
        assertEquals(Map.of(ALICE, true), snapshot.spendsGlobal());
    }

    @Test
    void skipsRowsWithAnInvalidUuid() throws SQLException {
        execute("INSERT INTO skysong_downtime(uuid, current_downtime) VALUES (?, ?)", "not-a-uuid", 7);
        execute("INSERT INTO skysong_downtime(uuid, current_downtime) VALUES (?, ?)", BOB.toString(), 1);
        assertEquals(Map.of(BOB, 1), repository.loadAll().globals());
    }

    @Test
    void saveGlobalInsertsThenUpdatesOneRow() throws SQLException {
        repository.saveGlobal(ALICE, 3);
        repository.saveGlobal(ALICE, 5);
        assertEquals(List.of(5), ints("SELECT current_downtime FROM skysong_downtime WHERE uuid = ?", ALICE.toString()));
    }

    @Test
    void saveCharacterKeepsProfilesApart() throws SQLException {
        repository.saveCharacter(new CharacterKey(ALICE, "Mage"), 3);
        repository.saveCharacter(new CharacterKey(ALICE, "Rogue"), 1);
        repository.saveCharacter(new CharacterKey(ALICE, "Mage"), 0);
        assertEquals(Map.of(new CharacterKey(ALICE, "Mage"), 0, new CharacterKey(ALICE, "Rogue"), 1), repository.loadAll().characters());
        assertEquals(List.of(2), ints("SELECT COUNT(*) FROM skysong_character_downtime"));
    }

    @Test
    void deleteCharacterRemovesOnlyThatProfile() {
        repository.saveCharacter(new CharacterKey(ALICE, "Mage"), 3);
        repository.saveCharacter(new CharacterKey(ALICE, "Rogue"), 3);
        repository.deleteCharacter(new CharacterKey(ALICE, "Mage"));
        assertEquals(Map.of(new CharacterKey(ALICE, "Rogue"), 3), repository.loadAll().characters());
    }

    @Test
    void savePreferenceInsertsThenUpdates() {
        repository.savePreference(ALICE, true);
        assertTrue(repository.loadAll().spendsGlobal().get(ALICE));
        repository.savePreference(ALICE, false);
        assertFalse(repository.loadAll().spendsGlobal().get(ALICE));
    }

    @Test
    void saveAllWritesEverything() {
        repository.saveGlobal(ALICE, 1);
        repository.saveAll(Map.of(ALICE, 4, BOB, 3), Map.of(new CharacterKey(BOB, "Default"), 3));
        DowntimeSnapshot snapshot = repository.loadAll();
        assertEquals(Map.of(ALICE, 4, BOB, 3), snapshot.globals());
        assertEquals(Map.of(new CharacterKey(BOB, "Default"), 3), snapshot.characters());
    }

    @Test
    void saveAllRollsBackOnFailure() throws SQLException {
        repository.saveGlobal(ALICE, 1);
        execute("DROP TABLE skysong_character_downtime");
        assertThrows(DowntimeStorageException.class,
                () -> repository.saveAll(Map.of(ALICE, 4), Map.of(new CharacterKey(ALICE, "Default"), 3)));
        assertEquals(List.of(1), ints("SELECT current_downtime FROM skysong_downtime"));
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void serviceChangesSurviveARestart() {
        DowntimeService service = new DowntimeService(repository, DowntimeSettings.defaults(), DowntimeChangeListener.NONE);
        service.load();
        service.addGlobal(ALICE, 1);
        service.removeCharacter(ALICE, "Default", 2);
        service.togglePreference(ALICE);
        service.resetAll(Map.of(BOB, List.of("Default")));

        DowntimeService restarted = new DowntimeService(repository, DowntimeSettings.defaults(), DowntimeChangeListener.NONE);
        restarted.load();
        assertEquals(7, restarted.getGlobal(ALICE));
        assertEquals(3, restarted.getCharacter(ALICE, "Default"));
        assertTrue(restarted.spendsGlobal(ALICE));
        assertEquals(6, restarted.getGlobal(BOB));
    }

    private void execute(String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.execute();
        }
    }

    private List<Integer> ints(String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            List<Integer> values = new ArrayList<>();
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    values.add(results.getInt(1));
                }
            }
            return values;
        }
    }
}
