package org.skysongdev.skysongstats.setup;

import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The default stats.yml and skills.yml, for the /setup tests.
 */
final class SetupTestData {
    private SetupTestData() {
    }

    static StatsConfig defaultConfig() {
        return StatsConfigLoader.load(bundled("stats.yml"), name -> true);
    }

    static SkillsConfig defaultSkills() {
        return SkillsConfigLoader.load(bundled("skills.yml"), name -> true);
    }

    private static YamlConfiguration bundled(String file) {
        try (InputStream in = SetupTestData.class.getClassLoader().getResourceAsStream(file);
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
