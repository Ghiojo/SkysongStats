package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

/** The stats and YAML helpers the injuries.yml and conditions.yml tests share. */
final class AfflictionTestFiles {
    // temp-hp has a '-' in its id, to check modifiers still split at the sign.
    static final StatDefinitions STATS = StatsConfigLoader.load(yaml("""
            stats:
              str: {aliases: [strength]}
              dex: {}
              con: {}
              temp-hp: {}
              hp: {type: bar, max: "60 + {con} * 10", start: max}
            """), name -> true).stats();

    private AfflictionTestFiles() {
    }

    static YamlConfiguration yaml(String text) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return config;
    }

    static void assertHasError(List<String> errors, String expectedPart) {
        if (errors.stream().noneMatch(error -> error.contains(expectedPart))) {
            throw new AssertionError("expected an error containing \"" + expectedPart + "\" but got " + errors);
        }
    }
}
