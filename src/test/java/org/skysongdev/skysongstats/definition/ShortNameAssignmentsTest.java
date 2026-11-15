package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link ShortNameAssignments}: short names plugins give stats
 * at runtime, over stats.yml's.
 */
class ShortNameAssignmentsTest {

    private static final String STATS = """
            stats:
              str: {name: Strength, aliases: [strength], short-name: STR}
              dex: {name: Dexterity}
              hp: {name: HP, type: bar, max: 10}
            """;

    private final ShortNameAssignments<String> assignments = new ShortNameAssignments<>();
    private final StatDefinitions config = load(STATS);

    private static StatDefinitions load(String yaml) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return StatsConfigLoader.load(file, material -> true).stats();
    }

    private static String shortName(StatDefinitions stats, String id) {
        return stats.get(id).orElseThrow().display().shortName();
    }

    private ShortNameAssignments.Assignment<String> assign(String owner, String stat, String shortName) {
        return assignments.assign(config, owner, stat, shortName).orElseThrow();
    }

    @Test
    void anAssignedShortNameShowsAndCommandsTakeIt() {
        ShortNameAssignments.Assignment<String> agility = assign("npcs", "dex", " Agi ");
        assertEquals("Agi", agility.shortName());
        assertEquals("dex", agility.statId());
        assertEquals("npcs", agility.owner());

        StatDefinitions stats = assignments.apply(config);
        assertEquals("Agi", shortName(stats, "dex"));
        assertTrue(stats.get("dex").orElseThrow().display().customShortName());
        assertEquals("dex", stats.find("AGI").orElseThrow().id());
        assertEquals("STR", shortName(stats, "str"), "other stats keep stats.yml's");
        assertEquals("Dexterity", shortName(config, "dex"), "the config itself is unchanged");
    }

    @Test
    void withoutAssignmentsTheConfigIsUsedAsItIs() {
        assertSame(config, assignments.apply(config));
    }

    @Test
    void refusesAnUnknownStatOrABlankShortName() {
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "luck", "Lk"));
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "dex", "  "));
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "dex", null));
        assertTrue(assignments.all().isEmpty());
    }

    @Test
    void refusesANameCommandsWouldReadAsAnotherStat() {
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "dex", "HP"), "another id");
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "dex", "Strength"), "another alias");
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "dex", "str"), "another short name");
        assign("npcs", "dex", "Agi");
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "hp", "agi"), "another assigned one");
        assertEquals(1, assignments.all().size());
    }

    @Test
    void mayRepeatItsOwnNamesAndNamesWithSpacesNeverClash() {
        assign("npcs", "str", "Strength");
        assign("npcs", "dex", "Str ength");
        assign("npcs", "hp", "Str ength");
        StatDefinitions stats = assignments.apply(config);
        assertEquals("Strength", shortName(stats, "str"));
        assertEquals("Str ength", shortName(stats, "hp"));
    }

    @Test
    void theNewestAssignmentWinsAndTheOneBeforeComesBack() {
        ShortNameAssignments.Assignment<String> first = assign("npcs", "str", "Mgt");
        ShortNameAssignments.Assignment<String> second = assign("crafting", "str", "Pow");
        assertEquals("Pow", shortName(assignments.apply(config), "str"));
        assertFalse(assignments.isActive(config, first));
        assertTrue(assignments.isActive(config, second));

        assertTrue(assignments.remove(second));
        assertFalse(assignments.remove(second), "already removed");
        assertTrue(second.isRemoved());
        assertEquals("Mgt", shortName(assignments.apply(config), "str"));

        assertTrue(assignments.remove(first));
        assertEquals("STR", shortName(assignments.apply(config), "str"));
    }

    @Test
    void aReplacedAssignmentFreesItsNameButNeverAStatsOwnNames() {
        assign("npcs", "dex", "Agi");
        assign("npcs", "dex", "Nimble");
        assertEquals("hp", assignments.apply(config).find("hp").orElseThrow().id());
        assign("npcs", "hp", "Agi");
        assertEquals("Agi", shortName(assignments.apply(config), "hp"));

        assign("npcs", "str", "Str");
        assign("npcs", "str", "Mgt");
        assertEquals(Optional.empty(), assignments.assign(config, "npcs", "dex", "STR"), "still stat str's id");
    }

    @Test
    void removingAnOwnerRemovesOnlyItsAssignments() {
        assign("npcs", "str", "Mgt");
        assign("npcs", "dex", "Agi");
        ShortNameAssignments.Assignment<String> crafting = assign("crafting", "hp", "Life");
        assertTrue(assignments.removeOwner("npcs"));
        assertFalse(assignments.removeOwner("npcs"));
        assertEquals(List.of(crafting), assignments.all());
        StatDefinitions stats = assignments.apply(config);
        assertEquals("STR", shortName(stats, "str"));
        assertEquals("Life", shortName(stats, "hp"));
    }

    @Test
    void assignmentsOutliveAReloadAndWaitWhileTheyDontFit() {
        ShortNameAssignments.Assignment<String> agility = assign("npcs", "dex", "Agi");
        StatDefinitions withoutDex = load("stats:\n  str: {short-name: STR}\n");
        assertTrue(assignments.apply(withoutDex).get("dex").isEmpty());
        assertFalse(assignments.isActive(withoutDex, agility));

        StatDefinitions agiTaken = load("stats:\n  dex: {}\n  agi: {}\n");
        assertEquals("dex", shortName(assignments.apply(agiTaken), "dex"), "clashes with the new stat agi");

        assertEquals("Agi", shortName(assignments.apply(config), "dex"), "back when it fits again");
        assertEquals(List.of(agility), assignments.all());
    }

    @Test
    void aClashAfterReloadLetsAnOlderAssignmentShow() {
        assign("npcs", "dex", "Nimble");
        assign("crafting", "dex", "Agi");
        StatDefinitions agiTaken = load("stats:\n  dex: {}\n  agi: {}\n");
        assertEquals("Nimble", shortName(assignments.apply(agiTaken), "dex"));
    }
}
