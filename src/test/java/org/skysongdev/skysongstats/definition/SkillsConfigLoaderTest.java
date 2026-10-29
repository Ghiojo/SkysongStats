package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests (no server needed) for {@link SkillsConfigLoader}, the
 * skills.yml checks in {@link SkillsValidator} and the lookups in
 * {@link SkillDefinitions} and {@link SkillLevels}.
 */
class SkillsConfigLoaderTest {

    private static final Set<String> ITEMS = Set.of("BONE", "WHEAT", "BARRIER");
    private static final String LEVELS = """
            levels:
              unskilled: {xp: 0}
              novice: {name: Novice, color: "<green>", xp: 16}
            """;

    private static SkillsConfig load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return SkillsConfigLoader.load(config, ITEMS::contains);
    }

    private static List<String> errors(String yaml) {
        return assertThrows(DefinitionException.class, () -> load(yaml)).getErrors();
    }

    private static void assertError(String yaml, String expectedPart) {
        List<String> errors = errors(yaml);
        assertTrue(errors.stream().anyMatch(error -> error.contains(expectedPart)),
                "expected an error containing \"" + expectedPart + "\" but got " + errors);
    }

    @Test
    void plainSkillsGetDefaults() {
        SkillDefinition skill = load(LEVELS + "skills:\n  fishing: {}\n").skills().get("fishing").orElseThrow();
        assertEquals("fishing", skill.name());
        assertEquals("fishing", skill.shortName());
        assertEquals("", skill.color());
        assertEquals(0, skill.baseline());
        assertTrue(skill.aliases().isEmpty());
        assertTrue(skill.setupOptions().isEmpty());
    }

    @Test
    void readsASkillWithEverySetting() {
        SkillDefinition skill = load(LEVELS + """
                skills:
                  tande:
                    name: "Transportation and Exploration"
                    short-name: "T&E"
                    color: "<green>"
                    aliases: [T&E, travel]
                    baseline: 5
                    setup: {icon: BONE, slot: 24, color: "<gold>", lore: [one, two]}
                """).skills().get("tande").orElseThrow();
        assertEquals("Transportation and Exploration", skill.name());
        assertEquals("T&E", skill.shortName());
        assertEquals("<green>", skill.color());
        assertEquals(List.of("t&e", "travel"), skill.aliases());
        assertEquals(5, skill.baseline());
        SkillSetup setup = skill.setupOptions().orElseThrow();
        assertEquals("BONE", setup.icon().material());
        assertEquals(24, setup.slot());
        assertEquals("<gold>", setup.color());
        assertEquals(List.of("one", "two"), setup.lore());
    }

    @Test
    void readsLevelsLowestFirst() {
        SkillLevels levels = load(LEVELS + "skills: {}\n").levels();
        assertEquals(List.of("unskilled", "novice"), levels.all().stream().map(SkillLevel::id).toList());
        SkillLevel novice = levels.get("novice").orElseThrow();
        assertEquals(new SkillLevel("novice", "Novice", "<green>", 16), novice);
        assertEquals("unskilled", levels.get("unskilled").orElseThrow().name());
    }

    @Test
    void theLevelIsTheHighestReached() {
        SkillLevels levels = load(LEVELS + "skills: {}\n").levels();
        assertEquals("unskilled", levels.levelFor(-3).id());
        assertEquals("unskilled", levels.levelFor(15).id());
        assertEquals("novice", levels.levelFor(16).id());
        assertEquals("novice", levels.levelFor(500).id());
        assertEquals(List.of("unskilled"), levels.reachedWith(0).stream().map(SkillLevel::id).toList());
        assertEquals(List.of("unskilled", "novice"), levels.reachedWith(16).stream().map(SkillLevel::id).toList());
    }

    @Test
    void readsSetupRules() {
        SkillsConfig config = load(LEVELS + """
                setup: {picks: 1, picked-level: Novice}
                skills:
                  fishing: {setup: {icon: BONE, slot: 10}}
                """);
        assertEquals(1, config.setup().picks());
        assertEquals("novice", config.setup().pickedLevel().id());
        assertEquals(List.of("fishing"), config.skills().setupSkills().stream().map(SkillDefinition::id).toList());
    }

    @Test
    void missingSetupRulesMeanNoPicks() {
        SkillSetupRules rules = load(LEVELS + "skills: {}\n").setup();
        assertEquals(0, rules.picks());
        assertEquals("unskilled", rules.pickedLevel().id());
    }

    @Test
    void readsTheBookAndFindsSkillsOnNoPage() {
        SkillsConfig config = load(LEVELS + """
                book:
                  gathering: {title: "<green>Gathering", skills: [farming, fishing]}
                skills:
                  fishing: {}
                  farming: {}
                  cooking: {}
                """);
        SkillBookPage page = config.book().getFirst();
        assertEquals("gathering", page.id());
        assertEquals("<green>Gathering", page.title());
        assertEquals(List.of("farming", "fishing"), page.skills());
        assertEquals(List.of("cooking"), config.skillsOnNoPage().stream().map(SkillDefinition::id).toList());
    }

    @Test
    void lookupsIgnoreCaseAndCoverIdsAndAliases() {
        SkillDefinitions skills = load(LEVELS + "skills:\n  tande: {aliases: [t&e]}\n").skills();
        assertEquals("tande", skills.find("TandE").orElseThrow().id());
        assertEquals("tande", skills.find(" T&E ").orElseThrow().id());
        assertTrue(skills.get("t&e").isEmpty());
        assertTrue(skills.find("unknown").isEmpty());
        assertTrue(skills.find(null).isEmpty());
    }

    @Test
    void definitionsCannotBeModified() {
        SkillsConfig config = load(LEVELS + "book: {p: {skills: [a]}}\nskills:\n  a: {aliases: [b]}\n");
        assertThrows(UnsupportedOperationException.class, () -> config.skills().all().clear());
        assertThrows(UnsupportedOperationException.class, () -> config.levels().all().clear());
        assertThrows(UnsupportedOperationException.class, () -> config.book().getFirst().skills().clear());
        assertThrows(UnsupportedOperationException.class, () -> config.skills().get("a").orElseThrow().aliases().clear());
    }

    @Test
    void reportsEveryMistakeAtOnce() {
        List<String> errors = errors("""
                levels: {}
                skills:
                  Bad: {}
                  fishing: {baseline: -1}
                """);
        assertEquals(3, errors.size(), errors.toString());
    }

    @Test
    void rejectsMissingOrBadLevels() {
        assertError("skills: {}\n", "levels: missing");
        assertError("levels:\n  novice: {xp: 16}\nskills: {}\n", "levels.novice.xp: the first level must start at 0 XP");
        assertError("levels:\n  a: {xp: 0}\n  b: {xp: 10}\n  c: {xp: 10}\nskills: {}\n", "levels.c.xp: must be more than b's 10");
        assertError("levels:\n  a: {xp: 0}\n  b: {xp: ten}\nskills: {}\n", "levels.b.xp: 'ten' is not a whole number");
        assertError("levels:\n  Novice: {xp: 0}\nskills: {}\n", "levels.Novice: ids may only use");
        assertError("levels:\n  a: 0\nskills: {}\n", "levels.a: must be a section");
    }

    @Test
    void rejectsBadSetupRules() {
        assertError(LEVELS + "setup: {picks: 1, picked-level: master}\nskills:\n  a: {setup: {icon: BONE, slot: 9}}\n",
                "setup.picked-level: 'master' is not a level id");
        assertError(LEVELS + "setup: {picks: -1}\nskills: {}\n", "setup.picks: can't be negative");
        assertError(LEVELS + "setup: {picks: 2}\nskills:\n  a: {setup: {icon: BONE, slot: 9}}\n  b: {}\n",
                "setup.picks: 2 picks but only 1 skill(s) have a 'setup' section");
    }

    @Test
    void rejectsAFileWithoutSkills() {
        assertError(LEVELS, "skills: missing");
    }

    @Test
    void rejectsBadIdsAndNumbers() {
        assertError(LEVELS + "skills:\n  Fishing: {}\n", "skills.Fishing: ids may only use");
        assertError(LEVELS + "skills:\n  fishing: 3\n", "skills.fishing: must be a section");
        assertError(LEVELS + "skills:\n  fishing: {baseline: -1}\n", "skills.fishing.baseline: can't be negative");
    }

    @Test
    void rejectsDuplicateAliases() {
        assertError(LEVELS + "skills:\n  a: {aliases: [x]}\n  b: {aliases: [x]}\n", "skills.b.aliases: 'x' is already used by skill 'a'");
        assertError(LEVELS + "skills:\n  a: {}\n  b: {aliases: [a]}\n", "'a' is already used by skill 'a'");
        assertError(LEVELS + "skills:\n  a: {aliases: [\"two words\"]}\n", "can't be empty or contain spaces");
        assertError(LEVELS + "skills:\n  a: {aliases: [\" \"]}\n", "can't be empty or contain spaces");
    }

    @Test
    void rejectsBadSetupSettings() {
        assertError(LEVELS + "skills:\n  a: {setup: {slot: 10}}\n", "skills.a.setup.icon: missing");
        assertError(LEVELS + "skills:\n  a: {setup: {icon: DIAMOND_ORE, slot: 10}}\n", "'DIAMOND_ORE' is not an item Material");
        assertError(LEVELS + "skills:\n  a: {setup: {icon: BONE}}\n", "skills.a.setup.slot: missing");
        assertError(LEVELS + "skills:\n  a: {setup: {icon: BONE, slot: 8}}\n", "must be between 9 and 44");
        assertError(LEVELS + "skills:\n  a: {setup: {icon: BONE, slot: 45}}\n", "must be between 9 and 44");
        assertError(LEVELS + """
                skills:
                  a: {setup: {icon: BONE, slot: 10}}
                  b: {setup: {icon: BONE, slot: 10}}
                """, "skills.b.setup.slot: slot 10 is already used by skill 'a'");
    }

    @Test
    void rejectsBadBookPages() {
        assertError(LEVELS + "book:\n  p: {skills: [fishing]}\nskills: {}\n", "book.p.skills: 'fishing' is not a skill id");
        assertError(LEVELS + "book:\n  p: {skills: [a]}\n  q: {skills: [a]}\nskills:\n  a: {}\n",
                "book.q.skills: 'a' is already on page 'p'");
        assertError(LEVELS + "book:\n  p: {skills: [a, a]}\nskills:\n  a: {}\n", "book.p.skills: 'a' is already on page 'p'");
        assertError(LEVELS + "book:\n  P: {}\nskills: {}\n", "book.P: ids may only use");
    }
}
