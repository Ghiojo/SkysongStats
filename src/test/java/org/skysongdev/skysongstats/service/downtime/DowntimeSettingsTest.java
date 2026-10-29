package org.skysongdev.skysongstats.service.downtime;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DowntimeSettingsTest {

    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(text);
        return config;
    }

    @Test
    void defaultsMatchTheOldConstants() {
        assertEquals(new DowntimeSettings(3, 3, 3, 9), DowntimeSettings.defaults());
    }

    @Test
    void missingSectionGivesDefaults() throws InvalidConfigurationException {
        assertEquals(DowntimeSettings.defaults(), DowntimeSettings.fromConfig(yaml("database: {}").getConfigurationSection("downtime")));
    }

    @Test
    void readsEveryKey() throws InvalidConfigurationException {
        YamlConfiguration config = yaml("""
                downtime:
                  character-max: 4
                  global-start: 2
                  global-recovery: 5
                  global-max: 12
                """);
        assertEquals(new DowntimeSettings(4, 2, 5, 12), DowntimeSettings.fromConfig(config.getConfigurationSection("downtime")));
    }

    @Test
    void missingOrNegativeKeysFallBackToDefaults() throws InvalidConfigurationException {
        YamlConfiguration config = yaml("""
                downtime:
                  character-max: -1
                  global-max: 20
                """);
        assertEquals(new DowntimeSettings(3, 3, 3, 20), DowntimeSettings.fromConfig(config.getConfigurationSection("downtime")));
    }

    @Test
    void bundledConfigHasTheDefaults() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("config.yml"), StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        assertEquals(DowntimeSettings.defaults(), DowntimeSettings.fromConfig(config.getConfigurationSection("downtime")));
        assertEquals(9, config.getInt("downtime.global-max"));
    }

    @Test
    void maxForEachPool() {
        DowntimeSettings settings = new DowntimeSettings(4, 2, 5, 12);
        assertEquals(12, settings.maxFor(DowntimePool.GLOBAL));
        assertEquals(4, settings.maxFor(DowntimePool.CHARACTER));
    }
}
