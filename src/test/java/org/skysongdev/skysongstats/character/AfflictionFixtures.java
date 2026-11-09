package org.skysongdev.skysongstats.character;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongstats.definition.ConditionDefinitions;
import org.skysongdev.skysongstats.definition.ConditionsConfigLoader;
import org.skysongdev.skysongstats.definition.InjuriesConfigLoader;
import org.skysongdev.skysongstats.definition.InjuryDefinitions;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

/**
 * The conditions and injuries the affliction tests share:
 * <pre>
 *   bleeding    no effects; its description shows the level
 *   weakened    gives dazed at level 2
 *   dazed       no effects
 *   broken-leg  gives weakened, has a message
 *   cut         gives bleeding at level 2
 *   deep-cut    gives bleeding at level 3
 *   bruise      gives nothing
 * </pre>
 */
public final class AfflictionFixtures {
    public static final StatDefinitions STATS = StatsConfigLoader.load(yaml("""
            stats:
              str: {}
              dex: {}
            """), name -> true).stats();
    public static final String CONDITIONS_FILE = """
            conditions:
              bleeding: {name: "Bleeding", description: "Loses $ blood"}
              weakened: {name: "Weakened", effects: {conditions: ["dazed:2"]}}
              dazed: {name: "Dazed"}
            """;
    public static final String INJURIES_FILE = """
            severities: [minor, severe]
            injuries:
              broken-leg:
                name: "Broken Leg"
                description: "Snapped"
                severity: severe
                message: "{character} now has a {injury}"
                effects: {conditions: [weakened]}
              cut: {name: "Cut", severity: minor, effects: {conditions: ["bleeding:2"]}}
              deep-cut: {name: "Deep Cut", severity: severe, effects: {conditions: ["bleeding:3"]}}
              bruise: {name: "Bruise", severity: minor}
            """;

    private AfflictionFixtures() {
    }

    public static ConditionDefinitions conditions(String file) {
        return ConditionsConfigLoader.load(yaml(file), STATS, name -> true);
    }

    public static ConditionDefinitions conditions() {
        return conditions(CONDITIONS_FILE);
    }

    public static InjuryDefinitions injuries(String file, ConditionDefinitions conditions) {
        return InjuriesConfigLoader.load(yaml(file), STATS, conditions, name -> true);
    }

    public static InjuryDefinitions injuries() {
        return injuries(INJURIES_FILE, conditions());
    }

    public static YamlConfiguration yaml(String text) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return config;
    }
}
