package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests (no server needed) for {@link ProficienciesConfigLoader}, the
 * checks in {@link ProficienciesValidator} and the lookups in
 * {@link ProficiencyDefinitions}, {@link ProficiencyTiers} and {@link ProficienciesConfig}.
 */
class ProficienciesConfigLoaderTest {

    private static final SkillDefinitions SKILLS = new SkillDefinitions(List.of(
            new SkillDefinition("martial", "Martial", "Martial", "", List.of(), 0, null),
            new SkillDefinition("medical", "Medical", "Medical", "", List.of("med"), 0, null)));
    private static final String TIERS = """
            cost: 4
            tiers:
              untrained:  {name: Untrained, color: "<dark_gray>"}
              proficient: {name: Proficient}
              adept:      {name: Adept, extra: 2}
              master:     {name: Master, extra: 3}
            """;

    private static ProficienciesConfig load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return ProficienciesConfigLoader.load(config, SKILLS, name -> true);
    }

    private static List<String> errors(String yaml) {
        return assertThrows(DefinitionException.class, () -> load(yaml)).getErrors();
    }

    private static void assertError(String yaml, String expectedPart) {
        List<String> errors = errors(yaml);
        assertTrue(errors.stream().anyMatch(error -> error.contains(expectedPart)),
                "expected an error containing \"" + expectedPart + "\" but got " + errors);
    }

    private static ProficiencyTier tier(ProficienciesConfig config, String id) {
        return config.tiers().get(id).orElseThrow();
    }

    @Test
    void readsAProficiencyWithEverySetting() {
        ProficiencyDefinition axes = load(TIERS + """
                proficiencies:
                  axes: {name: Axes, color: "<red>", skill: Martial, aliases: [Axe, hatchet], cost: 6}
                """).proficiencies().get("axes").orElseThrow();
        assertEquals(new ProficiencyDefinition("axes", "Axes", "<red>", "martial", List.of("axe", "hatchet"), 6,
                new ProficiencySetup(Icon.parse("BOOK"), List.of())), axes);
    }

    @Test
    void plainProficienciesGetDefaultsAndTheGlobalCost() {
        ProficiencyDefinition axes = load(TIERS + "proficiencies:\n  axes: {skill: martial}\n")
                .proficiencies().get("axes").orElseThrow();
        assertEquals("axes", axes.name());
        assertEquals("", axes.color());
        assertTrue(axes.aliases().isEmpty());
        assertEquals(4, axes.cost());
    }

    @Test
    void readsTheTiersInOrder() {
        ProficienciesConfig config = load(TIERS + "proficiencies:\n  axes: {skill: martial}\n");
        assertEquals(List.of(
                new ProficiencyTier("untrained", "Untrained", "<dark_gray>", 0),
                new ProficiencyTier("proficient", "Proficient", "", 0),
                new ProficiencyTier("adept", "Adept", "", 2),
                new ProficiencyTier("master", "Master", "", 3)), config.tiers().all());
        assertEquals("untrained", config.tiers().first().id());
        assertEquals("master", config.tiers().top().id());
    }

    @Test
    void eachTierCostsThePreviousOnePlusItsExtra() {
        ProficienciesConfig config = load(TIERS + """
                proficiencies:
                  axes: {skill: martial}
                  bows: {skill: martial, cost: 6}
                """);
        ProficiencyDefinition axes = config.proficiencies().get("axes").orElseThrow();
        ProficiencyDefinition bows = config.proficiencies().get("bows").orElseThrow();
        assertEquals(List.of(0, 4, 6, 9), config.tiers().all().stream().map(tier -> config.cost(axes, tier)).toList());
        assertEquals(List.of(0, 6, 8, 11), config.tiers().all().stream().map(tier -> config.cost(bows, tier)).toList());
    }

    @Test
    void tiersKnowWhatComesNextAndWhichIsHigher() {
        ProficienciesConfig config = load(TIERS + "proficiencies:\n  axes: {skill: martial}\n");
        ProficiencyTiers tiers = config.tiers();
        assertEquals("proficient", tiers.next(tier(config, "untrained")).orElseThrow().id());
        assertTrue(tiers.next(tier(config, "master")).isEmpty());
        assertTrue(tiers.isTop(tier(config, "master")));
        assertFalse(tiers.isTop(tier(config, "adept")));
        assertTrue(tiers.atLeast(tier(config, "adept"), tier(config, "proficient")));
        assertTrue(tiers.atLeast(tier(config, "adept"), tier(config, "adept")));
        assertFalse(tiers.atLeast(tier(config, "proficient"), tier(config, "adept")));
        assertTrue(tiers.get("grandmaster").isEmpty());
    }

    @Test
    void findsProficienciesByIdAliasAndSkill() {
        ProficienciesConfig config = load(TIERS + """
                proficiencies:
                  axes: {skill: martial, aliases: [axe]}
                  medicine: {skill: medical}
                  bows: {skill: martial}
                """);
        ProficiencyDefinitions proficiencies = config.proficiencies();
        assertEquals("axes", proficiencies.find(" AXE ").orElseThrow().id());
        assertEquals("axes", proficiencies.find("Axes").orElseThrow().id());
        assertTrue(proficiencies.get("axe").isEmpty());
        assertEquals(List.of("axes", "bows"), proficiencies.ofSkill("MARTIAL").stream().map(ProficiencyDefinition::id).toList());
        assertEquals(List.of("axes", "medicine", "bows"), proficiencies.all().stream().map(ProficiencyDefinition::id).toList());
    }

    @Test
    void aSkillAliasIsNotEnoughTheSkillMustBeNamedByItsId() {
        assertError(TIERS + "proficiencies:\n  medicine: {skill: med}\n",
                "proficiencies.medicine.skill: 'med' is not a skill id in skills.yml");
    }

    @Test
    void everyProficiencyNeedsAKnownSkill() {
        List<String> errors = errors(TIERS + """
                proficiencies:
                  axes: {name: Axes}
                  sailing: {skill: seafaring}
                """);
        assertEquals(List.of(
                "proficiencies.axes.skill: missing, every proficiency belongs to a skill from skills.yml",
                "proficiencies.sailing.skill: 'seafaring' is not a skill id in skills.yml"), errors);
    }

    @Test
    void costsMustBeWholeNumbersOfAtLeastOne() {
        assertError("tiers: {a: {}, b: {}}\nproficiencies: {}\n", "cost: missing, the points the first paid tier costs");
        assertError("cost: 0\ntiers: {a: {}, b: {}}\nproficiencies: {}\n", "cost: must be at least 1");
        assertError("cost: lots\ntiers: {a: {}, b: {}}\nproficiencies: {}\n", "cost: 'lots' is not a whole number");
        assertError(TIERS + "proficiencies:\n  axes: {skill: martial, cost: -2}\n",
                "proficiencies.axes.cost: must be at least 1");
    }

    @Test
    void needsAtLeastTwoTiers() {
        assertError("cost: 4\ntiers: {untrained: {}}\nproficiencies: {}\n", "tiers: proficiencies.yml needs at least two tiers");
        assertError("cost: 4\nproficiencies: {}\n", "tiers: proficiencies.yml needs at least two tiers");
    }

    @Test
    void onlyTiersAfterTheFirstTwoHaveAnExtra() {
        List<String> errors = errors("""
                cost: 4
                tiers:
                  untrained: {extra: 1}
                  proficient: {extra: 1}
                  adept: {extra: -1}
                proficiencies: {}
                """);
        assertEquals(List.of(
                "tiers.untrained.extra: only the tiers after the first two have an extra; the first tier is free",
                "tiers.proficient.extra: only the tiers after the first two have an extra; this tier costs 'cost'",
                "tiers.adept.extra: can't be negative"), errors);
    }

    @Test
    void theProficienciesSectionIsRequired() {
        assertError(TIERS, "proficiencies: missing");
    }

    @Test
    void badIdsAndEntriesAreReported() {
        assertError(TIERS + "proficiencies:\n  Long Blade: {skill: martial}\n",
                "proficiencies.Long Blade: ids may only use lowercase letters");
        assertError(TIERS + "proficiencies:\n  axes: martial\n", "proficiencies.axes: must be a section with its settings");
    }

    @Test
    void idsAndAliasesCantBeSubcommands() {
        assertError(TIERS + "proficiencies:\n  confirm: {skill: martial}\n",
                "proficiencies.confirm: 'confirm' is a /proficiency subcommand, pick another id");
        assertError(TIERS + "proficiencies:\n  axes: {skill: martial, aliases: [focus]}\n",
                "proficiencies.axes.aliases: 'focus' is a /proficiency subcommand");
    }

    @Test
    void aliasesMustBeUniqueAndHaveNoSpaces() {
        assertError(TIERS + """
                proficiencies:
                  axes: {skill: martial, aliases: [chop]}
                  bows: {skill: martial, aliases: [chop]}
                """, "proficiencies.bows.aliases: 'chop' is already used by proficiency 'axes'");
        assertError(TIERS + """
                proficiencies:
                  axes: {skill: martial}
                  bows: {skill: martial, aliases: [axes]}
                """, "proficiencies.bows.aliases: 'axes' is already used by proficiency 'axes'");
        assertError(TIERS + "proficiencies:\n  axes: {skill: martial, aliases: [\"big axe\"]}\n",
                "proficiencies.axes.aliases: 'big axe' can't be empty or contain spaces");
    }

    @Test
    void everyMistakeIsReportedAtOnce() {
        List<String> errors = errors("""
                cost: 0
                tiers: {untrained: {}}
                proficiencies:
                  axes: {}
                  view: {skill: martial}
                """);
        assertEquals(4, errors.size(), errors.toString());
    }

    @Test
    void readsTheSetupSection() {
        ProficienciesConfig config = load(TIERS + """
                setup:
                  trained: [Axes]
                  picks: {medical: 1}
                  skill-picks: {Martial: 2, medical: 0}
                proficiencies:
                  axes: {skill: martial}
                  medicine: {skill: medical}
                """);
        assertEquals(new ProficiencySetupRules(List.of("axes"), Map.of("medical", 1),
                Map.of("martial", 2, "medical", 0)), config.setup());
    }

    @Test
    void withoutASetupSectionNewCharactersGetNothing() {
        assertEquals(ProficiencySetupRules.none(), load(TIERS + "proficiencies:\n  axes: {skill: martial}\n").setup());
    }

    @Test
    void picksAddUpForTheSkillsPickedOnTheSkillScreen() {
        ProficiencySetupRules setup = new ProficiencySetupRules(List.of(), Map.of("medical", 1),
                Map.of("martial", 2, "medical", 1, "crafting", 0));
        assertEquals(Map.of("medical", 1), setup.picksFor(List.of()));
        assertEquals(Map.of("medical", 2, "martial", 2), setup.picksFor(List.of("martial", "medical")));
        assertEquals(Map.of("medical", 1), setup.picksFor(List.of("crafting")));
    }

    @Test
    void theSetupSectionNamesKnownSkillsAndProficiencies() {
        List<String> errors = errors(TIERS + """
                setup:
                  trained: [axes, juggling]
                  picks: {crafting: 1}
                  skill-picks: {med: 1, martial: -1}
                proficiencies:
                  axes: {skill: martial}
                """);
        assertTrue(errors.contains("setup.trained: 'juggling' is not a proficiency id"), errors.toString());
        assertTrue(errors.contains("setup.picks.crafting: 'crafting' is not a skill id in skills.yml"),
                errors.toString());
        assertTrue(errors.contains("setup.skill-picks.med: 'med' is not a skill id in skills.yml"), errors.toString());
        assertTrue(errors.contains("setup.skill-picks.martial: can't be negative"), errors.toString());
    }

    @Test
    void setupPartsMustBeSections() {
        assertError(TIERS + "setup: 3\nproficiencies:\n  axes: {skill: martial}\n",
                "setup: must be a section");
        assertError(TIERS + "setup:\n  picks: 2\nproficiencies:\n  axes: {skill: martial}\n",
                "setup.picks: must be a section of skill id: number of picks");
    }

    @Test
    void theSetupScreenMustFitEveryProficiencyItCanShow() {
        String martial = IntStream.range(0, ProficiencySetupRules.SCREEN_SLOTS)
                .mapToObj(i -> "  p" + i + ": {skill: martial}\n")
                .collect(Collectors.joining());
        String yaml = TIERS + "setup:\n  skill-picks: {martial: 1}\n  trained: [m0]\nproficiencies:\n" + martial
                + "  m0: {skill: medical}\n";
        assertError(yaml, "setup: the /setup screen can show 36 proficiencies, but the trained ones and "
                + "the skills with picks have 37");
    }

    @Test
    void readsTheSetupIconAndLore() {
        ProficiencySetup setup = load(TIERS + """
                proficiencies:
                  axes: {skill: martial, icon: iron_axe, lore: ["<gray>Chop"]}
                  bows: {skill: martial, icon: "model:skysong:bow"}
                """).proficiencies().get("axes").orElseThrow().setup();
        assertEquals(new ProficiencySetup(Icon.parse("IRON_AXE"), List.of("<gray>Chop")), setup);
    }

    @Test
    void setupIconsMustBeItems() {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(TIERS + "proficiencies:\n  axes: {skill: martial, icon: NOT_AN_ITEM}\n");
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        List<String> errors = assertThrows(DefinitionException.class,
                () -> ProficienciesConfigLoader.load(config, SKILLS, "BOOK"::equals)).getErrors();
        assertEquals(List.of("proficiencies.axes.icon: 'NOT_AN_ITEM' is not an item Material"), errors);
    }
}
