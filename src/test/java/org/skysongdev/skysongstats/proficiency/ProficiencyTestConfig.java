package org.skysongdev.skysongstats.proficiency;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongstats.character.ProficiencyChangeListener;
import org.skysongdev.skysongstats.character.ProficiencyFocusChange;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficienciesConfigLoader;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.storage.UnsavedProficiencyRepository;

// A small proficiencies.yml for the proficiency package tests.
final class ProficiencyTestConfig {
    private static final String SKILLS = """
            levels:
              unskilled: {xp: 0}
            skills:
              martial: {}
            """;
    private static final String PROFICIENCIES = """
            cost: 4
            tiers:
              untrained: {name: Untrained}
              proficient: {name: Proficient}
              adept: {name: Adept, extra: 2}
              master: {name: Master, extra: 2}
            proficiencies:
              axes: {name: Axes, skill: martial}
              bows: {name: Bows, skill: martial}
            """;

    private ProficiencyTestConfig() {
    }

    static ProficienciesConfig load() {
        try {
            YamlConfiguration skills = new YamlConfiguration();
            skills.loadFromString(SKILLS);
            YamlConfiguration file = new YamlConfiguration();
            file.loadFromString(PROFICIENCIES);
            return ProficienciesConfigLoader.load(file, SkillsConfigLoader.load(skills, name -> true).skills());
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    // A service whose tier changes go to permissions, saving nothing.
    static ProficiencyService service(ProficienciesConfig config, ProficiencyTierPermissions permissions) {
        return new ProficiencyService(config, new UnsavedProficiencyRepository(line -> { }),
                new ProficiencyChangeListener() {
                    @Override
                    public void onTierChange(ProficiencyTierChange change) {
                        permissions.onTierChange(change);
                    }

                    @Override
                    public void onFocusChange(ProficiencyFocusChange change) {
                        // Focus changes give no nodes.
                    }
                }, line -> { });
    }
}
