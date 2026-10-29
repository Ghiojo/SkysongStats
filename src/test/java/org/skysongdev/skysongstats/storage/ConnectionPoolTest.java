package org.skysongdev.skysongstats.storage;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link DatabaseSettings} and the pool settings built by
 * {@link ConnectionPool} (without connecting to MySQL).
 */
class ConnectionPoolTest {

    // Made up at runtime so no password is written in the source.
    private static final String PASSWORD = UUID.randomUUID().toString();

    private static DatabaseSettings fromYaml(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return DatabaseSettings.fromConfig(config.getConfigurationSection("database"));
    }

    @Test
    void readsTheDatabaseSectionOfConfigYml() throws InvalidConfigurationException {
        DatabaseSettings settings = fromYaml("""
                database:
                  host: "db.example"
                  port: "3307"
                  user: "skysong"
                  password: "%s"
                  database_name: "skysong_titles"
                """.formatted(PASSWORD));
        assertEquals(new DatabaseSettings("db.example", "3307", "skysong_titles", "skysong", PASSWORD), settings);
        assertEquals("jdbc:mysql://db.example:3307/skysong_titles", settings.jdbcUrl());
    }

    @Test
    void missingKeysUseTheDefaultsFromConfigYml() throws InvalidConfigurationException {
        DatabaseSettings settings = fromYaml("database:\n  database_name: stats\n");
        assertEquals("jdbc:mysql://localhost:3306/stats", settings.jdbcUrl());
        assertEquals("root", settings.user());
        assertNull(settings.password());
    }

    @Test
    void aMissingSectionIsAClearError() {
        NullPointerException error = assertThrows(NullPointerException.class, () -> DatabaseSettings.fromConfig(null));
        assertTrue(error.getMessage().contains("database:"));
    }

    @Test
    void toStringHidesThePassword() {
        String text = new DatabaseSettings("h", "1", "d", "u", PASSWORD).toString();
        assertFalse(text.contains(PASSWORD), text);
        assertTrue(text.contains("u@h:1/d"), text);
    }

    @Test
    void poolUsesTheSettings() {
        HikariConfig config = ConnectionPool.config(new DatabaseSettings("h", "1", "d", "u", PASSWORD));
        assertEquals("jdbc:mysql://h:1/d", config.getJdbcUrl());
        assertEquals("u", config.getUsername());
        assertEquals(PASSWORD, config.getPassword());
        assertEquals(ConnectionPool.MAXIMUM_POOL_SIZE, config.getMaximumPoolSize());
        assertEquals("SkysongStats", config.getPoolName());
    }
}
