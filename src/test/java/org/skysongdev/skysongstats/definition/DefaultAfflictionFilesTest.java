package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the conditions.yml and injuries.yml shipped in the jar load against
 * the shipped stats.yml. They start empty (the server's own come from
 * /ssimport chattweaks or are written by hand) with SkysongChatTweaks' severities.
 */
class DefaultAfflictionFilesTest {

    private static YamlConfiguration bundled(String name) throws IOException {
        try (InputStream in = DefaultAfflictionFilesTest.class.getClassLoader().getResourceAsStream(name);
             Reader reader = new InputStreamReader(in, UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    @Test
    void theDefaultFilesLoad() throws IOException {
        StatDefinitions stats = StatsConfigLoader.load(bundled("stats.yml"), name -> true).stats();
        ConditionDefinitions conditions = ConditionsConfigLoader.load(bundled("conditions.yml"), stats, name -> true);
        InjuryDefinitions injuries = InjuriesConfigLoader.load(bundled("injuries.yml"), stats, conditions, name -> true);
        assertEquals(List.of(), conditions.all());
        assertEquals(List.of(), injuries.all());
        assertEquals(List.of("minor", "moderate", "severe", "permanent"), injuries.severities());
    }
}
