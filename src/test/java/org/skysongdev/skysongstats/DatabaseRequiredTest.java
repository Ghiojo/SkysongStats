package org.skysongdev.skysongstats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The plugin can't work without MySQL, so it must disable itself when it can't reach it. */
class DatabaseRequiredTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // Port 1 refuses every connection, so this never reaches a MySQL that runs nearby.
    private static YamlConfiguration configWithoutMySql() throws Exception {
        try (InputStream in = Objects.requireNonNull(
                SkysongStats.class.getClassLoader().getResourceAsStream("config.yml"), "config.yml")) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            config.set("database.host", "127.0.0.1");
            config.set("database.port", "1");
            return config;
        }
    }

    @Test
    void disablesItselfWhenMySqlCannotBeReached() throws Exception {
        SkysongStats plugin = MockBukkit.loadWithConfig(SkysongStats.class, configWithoutMySql());

        assertFalse(plugin.isEnabled(), "SkysongStats must not run without its database");
        assertNull(plugin.getStatService(), "nothing is loaded without the database");
    }
}
