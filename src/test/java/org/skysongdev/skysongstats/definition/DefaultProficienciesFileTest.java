package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the proficiencies.yml shipped in the jar loads against the shipped
 * skills.yml and gives Skysong's proficiencies (Agustín's list, Oct 30), each in
 * its skill, with the tiers and costs from the master plan.
 */
class DefaultProficienciesFileTest {

    private static ProficienciesConfig config;

    // id -> name, skill the default proficiencies.yml must give.
    private static final Map<String, List<String>> DEFAULT_PROFICIENCIES = new LinkedHashMap<>();

    static {
        martial("unarmored", "Unarmored");
        martial("light_armor", "Light Armor");
        martial("medium_armor", "Medium Armor");
        martial("heavy_armor", "Heavy Armor");
        martial("arcane_arms", "Arcane Arms");
        martial("axes", "Axes");
        martial("bows", "Bows");
        martial("crossbows", "Crossbows");
        martial("firearms", "Firearms");
        martial("picks", "Picks");
        martial("polearms", "Polearms");
        martial("spears", "Spears");
        martial("whips", "Whips");
        martial("projectile", "Projectile");
        martial("blunt", "Blunt");
        martial("long_blade", "Long Blade");
        martial("short_blade", "Short Blade");
        martial("exotic", "Exotic");
        // Logistics & Economics is the skill with id economical in skills.yml.
        DEFAULT_PROFICIENCIES.put("perception", List.of("Perception", "economical"));
        DEFAULT_PROFICIENCIES.put("surface_vehicle", List.of("Surface Vehicle", "economical"));
        DEFAULT_PROFICIENCIES.put("air_vehicle", List.of("Air Vehicle", "economical"));
        DEFAULT_PROFICIENCIES.put("surface_mount", List.of("Surface Mount", "economical"));
        DEFAULT_PROFICIENCIES.put("air_mount", List.of("Air Mount", "economical"));
        DEFAULT_PROFICIENCIES.put("survival", List.of("Survival", "economical"));
        DEFAULT_PROFICIENCIES.put("medicine", List.of("Medicine", "medical"));
        DEFAULT_PROFICIENCIES.put("sleight_of_hand", List.of("Sleight of Hand", "entertainment"));
        DEFAULT_PROFICIENCIES.put("stealth", List.of("Stealth", "entertainment"));
        DEFAULT_PROFICIENCIES.put("performance", List.of("Performance", "entertainment"));
    }

    private static void martial(String id, String name) {
        DEFAULT_PROFICIENCIES.put(id, List.of(name, "martial"));
    }

    @BeforeAll
    static void loadDefaultFiles() throws Exception {
        // The skill icons are not what this test is about, so any Material is accepted.
        SkillsConfig skills = SkillsConfigLoader.load(bundled("skills.yml"), name -> true);
        config = ProficienciesConfigLoader.load(bundled("proficiencies.yml"), skills.skills(), name -> true);
    }

    private static YamlConfiguration bundled(String name) throws Exception {
        try (InputStream in = DefaultProficienciesFileTest.class.getClassLoader().getResourceAsStream(name);
             Reader reader = new InputStreamReader(in, UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    @Test
    void hasSkysongsProficienciesInOrder() {
        assertEquals(List.copyOf(DEFAULT_PROFICIENCIES.keySet()),
                config.proficiencies().all().stream().map(ProficiencyDefinition::id).toList());
    }

    @Test
    void namesAndSkillsMatchTheList() {
        for (Map.Entry<String, List<String>> expected : DEFAULT_PROFICIENCIES.entrySet()) {
            ProficiencyDefinition proficiency = config.proficiencies().get(expected.getKey()).orElseThrow();
            assertEquals(expected.getValue().get(0), proficiency.name(), expected.getKey());
            assertEquals(expected.getValue().get(1), proficiency.skill(), expected.getKey());
            assertFalse(proficiency.color().isEmpty(), expected.getKey());
        }
    }

    @Test
    void theFourTiersCostFourSixAndEight() {
        assertEquals(List.of("untrained", "proficient", "adept", "master"),
                config.tiers().all().stream().map(ProficiencyTier::id).toList());
        assertEquals(List.of("Untrained", "Proficient", "Adept", "Master"),
                config.tiers().all().stream().map(ProficiencyTier::name).toList());
        for (ProficiencyDefinition proficiency : config.proficiencies().all()) {
            assertEquals(List.of(0, 4, 6, 8),
                    config.tiers().all().stream().map(tier -> config.cost(proficiency, tier)).toList(), proficiency.id());
        }
    }

    @Test
    void eachPickedSkillGivesOneExtraPickOfItsProficiencies() {
        assertEquals(new ProficiencySetupRules(List.of(), Map.of(),
                Map.of("martial", 1, "economical", 1, "medical", 1, "entertainment", 1)), config.setup());
        for (ProficiencyDefinition proficiency : config.proficiencies().all()) {
            assertEquals(Icon.parse(ProficiencySetup.DEFAULT_ICON), proficiency.setup().icon(), proficiency.id());
        }
    }
}
