package org.skysongdev.skysongstats.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.StatsManager;
import org.skysongdev.skysongstats.Utils.Utils;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks what {@link Database#dumpDatabaseData()} puts in memory on startup.
 * Runs against a temporary SQLite database instead of MySQL.
 *
 * The plugin is loaded on a MockBukkit server because {@link Utils} builds its
 * static NamespacedKeys from {@code SkysongStats.getPlugin()}. Its own MySQL
 * connection fails in tests; onEnable() logs that and carries on.
 */
class DatabaseLoadingTest {

    private static final String PLAYER = "00000000-0000-0000-0000-000000000001";

    private Connection connection;
    private Database database;

    /** Uses the test connection instead of MySQL. */
    private static class TestDatabase extends Database {
        private final Connection connection;

        TestDatabase(Connection connection) {
            super("", "", "", "", "");
            this.connection = connection;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        MockBukkit.mock();
        MockBukkit.load(SkysongStats.class);
        // The default data source opens a private temporary database that is deleted on close.
        connection = new SQLiteDataSource().getConnection();
        createTables(connection);
        database = new TestDatabase(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        try {
            if (connection != null) {
                connection.close();
            }
        } finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void eachStoredModifierIsLoadedOnce() throws SQLException {
        insertStats(PLAYER, "Default");
        insertModifier(PLAYER, "Default", 0, Utils.StaticStats.STRENGTH, 2);
        insertModifier(PLAYER, "Default", 1, Utils.StaticStats.MAXHP, 10);

        database.dumpDatabaseData();

        PlayerStats stats = loadedStats(PLAYER, "Default");
        assertEquals(2, stats.getModifiers().size());
        assertEquals(2, stats.getStrengthModifiers());
        assertEquals(10, stats.getMaxHPModifiers());
    }

    @Test
    void loadingTwiceDoesNotStackModifiers() throws SQLException {
        insertStats(PLAYER, "Default");
        insertModifier(PLAYER, "Default", 0, Utils.StaticStats.DEXTERITY, 3);

        database.dumpDatabaseData();
        database.dumpDatabaseData();

        PlayerStats stats = loadedStats(PLAYER, "Default");
        assertEquals(1, stats.getModifiers().size());
        assertEquals(3, stats.getDexterityModifiers());
    }

    @Test
    void modifiersStayOnTheirOwnProfile() throws SQLException {
        insertStats(PLAYER, "Default");
        insertStats(PLAYER, "Second");
        insertModifier(PLAYER, "Default", 0, Utils.StaticStats.FOCUS, 1);
        insertModifier(PLAYER, "Second", 0, Utils.StaticStats.FOCUS, 4);
        insertModifier(PLAYER, "Second", 1, Utils.StaticStats.SPEED, -1);

        database.dumpDatabaseData();

        PlayerStats first = loadedStats(PLAYER, "Default");
        PlayerStats second = loadedStats(PLAYER, "Second");
        assertEquals(1, first.getModifiers().size());
        assertEquals(1, first.getFocusModifiers());
        assertEquals(2, second.getModifiers().size());
        assertEquals(4, second.getFocusModifiers());
        assertEquals(-1, second.getSpeedModifiers());
    }

    @Test
    void profileWithoutModifiersLoadsWithAnEmptyList() throws SQLException {
        insertStats(PLAYER, "Default");

        database.dumpDatabaseData();

        assertTrue(loadedStats(PLAYER, "Default").getModifiers().isEmpty());
    }

    private static PlayerStats loadedStats(String uuid, String profile) {
        for (PlayerStats stats : StatsManager.statsProfileList) {
            if (stats.getUuid().equals(uuid) && stats.getProfile().equals(profile)) {
                return stats;
            }
        }
        return fail("No stats loaded for " + uuid + " / " + profile);
    }

    private void insertStats(String uuid, String profile) throws SQLException {
        PlayerStats stats = new PlayerStats(uuid, profile);
        database.createStatData(stats);
    }

    private void insertModifier(String uuid, String profile, int modid, Utils.StaticStats stat, int value) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO skysong_modifiers(uuid, profile, modid, stat, modifier) VALUES (?, ?, ?, ?, ?)");
        statement.setString(1, uuid);
        statement.setString(2, profile);
        statement.setInt(3, modid);
        statement.setInt(4, Utils.toInt(stat));
        statement.setInt(5, value);
        statement.executeUpdate();
        statement.close();
    }

    /** Same tables as {@link Database#initializeDatabase()}, in SQLite syntax. */
    private static void createTables(Connection connection) throws SQLException {
        List<String> skillColumns = new ArrayList<>();
        for (Utils.Skills skill : Utils.Skills.values()) {
            skillColumns.add(skill.toString().toLowerCase() + " INT");
        }
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE skysong_stats(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), profile varchar(20), strength INT, dexterity INT, focus INT, constitution INT, speed INT, arcrot INT, potion INT, fictis INT, hp INT, temphp INT, ac INT)");
        statement.execute("CREATE TABLE skysong_active_stats(uuid varchar(36) primary key, profile varchar(20))");
        statement.execute("CREATE TABLE skysong_modifiers(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), profile varchar(20), modid INT, stat INT, modifier INT)");
        statement.execute("CREATE TABLE skysong_skills(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), profile varchar(20), " + String.join(", ", skillColumns) + ")");
        statement.execute("CREATE TABLE skysong_setup(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), profile varchar(20), setup BOOLEAN)");
        statement.execute("CREATE TABLE skysong_characters(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), profile varchar(20), name varchar(100), age varchar(20), gender varchar(20), ancestry varchar(50), pronouns varchar(20), description varchar(500))");
        statement.execute("CREATE TABLE skysong_downtime(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), current_downtime INT)");
        statement.execute("CREATE TABLE skysong_character_downtime(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid varchar(36), profile varchar(20), current_downtime INT)");
        statement.execute("CREATE TABLE skysong_downtime_preferences(uuid varchar(36) primary key, isGlobal BOOLEAN)");
        statement.close();
    }
}
