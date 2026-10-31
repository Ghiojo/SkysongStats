package org.skysongdev.skysongstats.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stat tables as the plugin opens them, on an in-memory H2 database in MySQL
 * mode instead of MySQL.
 */
class StatStorageTest {
    private static HikariDataSource h2Pool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return new HikariDataSource(config);
    }

    @Test
    void savesInTheBackgroundAndClosingWaitsForTheSaves() {
        List<String> log = new ArrayList<>();
        HikariDataSource pool = h2Pool();
        ProfileKey alice = new ProfileKey(UUID.randomUUID(), "Default");
        StatStorage storage = new StatStorage(pool, log::add);
        storage.repository().createTables();
        storage.repository().saveValue(alice, "str", 3);
        storage.close();

        assertTrue(pool.isClosed());
        assertTrue(log.isEmpty(), log.toString());
    }

    @Test
    void savedValuesAreLoadedBack() {
        HikariDataSource pool = h2Pool();
        String url = pool.getJdbcUrl();
        ProfileKey alice = new ProfileKey(UUID.randomUUID(), "Default");
        StatStorage storage = new StatStorage(pool, line -> { });
        storage.repository().createTables();
        storage.repository().saveValue(alice, "str", 3);
        storage.close();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        try (HikariDataSource again = new HikariDataSource(config)) {
            assertEquals(3, new SqlStatRepository(again).loadAll().get(alice).values().get("str"));
        }
    }

    @Test
    void skillXpIsSavedThroughTheSamePool() {
        HikariDataSource pool = h2Pool();
        String url = pool.getJdbcUrl();
        ProfileKey alice = new ProfileKey(UUID.randomUUID(), "Default");
        StatStorage storage = new StatStorage(pool, line -> { });
        storage.skillRepository().createTables();
        storage.skillRepository().saveXp(alice, "farming", 16);
        storage.close();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        try (HikariDataSource again = new HikariDataSource(config)) {
            assertEquals(16, new SqlSkillRepository(again).loadAll().get(alice).get("farming"));
        }
    }

    @Test
    void proficienciesAreSavedThroughTheSamePool() {
        HikariDataSource pool = h2Pool();
        String url = pool.getJdbcUrl();
        ProfileKey alice = new ProfileKey(UUID.randomUUID(), "Default");
        StatStorage storage = new StatStorage(pool, line -> { });
        storage.proficiencyRepository().createTables();
        storage.proficiencyRepository().saveTier(alice, "axes", "adept");
        storage.proficiencyRepository().saveFocus(alice, new StoredFocus("axes", 2));
        storage.close();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        try (HikariDataSource again = new HikariDataSource(config)) {
            StoredProficiencies saved = new SqlProficiencyRepository(again).loadAll().get(alice);
            assertEquals("adept", saved.tiers().get("axes"));
            assertEquals(new StoredFocus("axes", 2), saved.focus());
        }
    }
}
