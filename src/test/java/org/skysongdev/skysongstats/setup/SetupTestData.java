package org.skysongdev.skysongstats.setup;

import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The default stats.yml, for the /setup tests.
 */
final class SetupTestData {
    private SetupTestData() {
    }

    static StatsConfig defaultConfig() {
        try (InputStream in = SetupTestData.class.getClassLoader().getResourceAsStream("stats.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return StatsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
