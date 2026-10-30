package org.skysongdev.skysongstats.skill;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;

// A small skills.yml for the skill package tests.
final class SkillTestConfig {
    static final String SKILLS = """
            levels:
              unskilled: {name: Unskilled, xp: 0}
              novice: {name: Novice, xp: 16}
              competent: {name: Competent, xp: 40}
              expert: {name: Expert, xp: 128}
            skills:
              farming: {name: Farming}
              tande: {name: "Transportation and Exploration"}
            """;

    private SkillTestConfig() {
    }

    static SkillsConfig load(String yaml) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return SkillsConfigLoader.load(file, name -> true);
    }

    static SkillsConfig load() {
        return load(SKILLS);
    }
}
