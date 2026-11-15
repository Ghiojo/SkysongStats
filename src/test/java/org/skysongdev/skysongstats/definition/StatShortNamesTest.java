package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for the {@code short-name} of a stat in stats.yml: how it is
 * read, checked, and found by commands.
 */
class StatShortNamesTest {

    private static StatsConfig load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return StatsConfigLoader.load(config, material -> true);
    }

    private static List<String> errors(String yaml) {
        return assertThrows(DefinitionException.class, () -> load(yaml)).getErrors();
    }

    private static StatDisplay display(String yaml, String id) {
        return load(yaml).stats().get(id).orElseThrow().display();
    }

    @Test
    void withoutShortNameItIsTheNameAndCommandsDontTakeIt() {
        StatDisplay display = display("stats:\n  str: {name: Strength}\n", "str");
        assertEquals("Strength", display.shortName());
        assertFalse(display.customShortName());
        assertEquals(Optional.empty(), display.shortNameInput());
    }

    @Test
    void readsTheShortNameAsWritten() {
        StatDisplay display = display("stats:\n  str: {name: Strength, short-name: \" STR \"}\n", "str");
        assertEquals("STR", display.shortName());
        assertTrue(display.customShortName());
        assertEquals(Optional.of("str"), display.shortNameInput());
    }

    @Test
    void anEmptyShortNameIsAMistake() {
        assertEquals(List.of("stats.str.short-name: can't be empty (leave it out to use the name)"),
                errors("stats:\n  str: {short-name: \"\"}\n"));
    }

    @Test
    void commandsFindAStatByItsShortNameIgnoringCase() {
        StatDefinitions stats = load("stats:\n  strength: {short-name: Str}\n  dex: {}\n").stats();
        assertEquals("strength", stats.find("STR").orElseThrow().id());
        assertEquals("strength", stats.find("str").orElseThrow().id());
        assertTrue(stats.get("str").isEmpty(), "get only takes ids");
    }

    @Test
    void aShortNameMayRepeatItsOwnIdOrAlias() {
        StatDefinitions stats = load("stats:\n  hp: {short-name: HP}\n  str: {aliases: [strong], short-name: Strong}\n").stats();
        assertEquals("hp", stats.find("hp").orElseThrow().id());
        assertEquals("str", stats.find("strong").orElseThrow().id());
    }

    @Test
    void aShortNameCantBeAnotherStatsIdAliasOrShortName() {
        assertEquals(List.of("stats.dex.short-name: 'STR' is already a name of stat 'str'"
                        + " (commands accept short names)"),
                errors("stats:\n  str: {}\n  dex: {short-name: STR}\n"));
        assertEquals(List.of("stats.dex.short-name: 'Strong' is already a name of stat 'str'"
                        + " (commands accept short names)"),
                errors("stats:\n  str: {aliases: [strong]}\n  dex: {short-name: Strong}\n"));
        assertEquals(List.of("stats.dex.short-name: 'S' is already a name of stat 'str'"
                        + " (commands accept short names)"),
                errors("stats:\n  str: {short-name: S}\n  dex: {short-name: S}\n"));
    }

    @Test
    void anAliasWrittenAfterAShortNameStillClashes() {
        List<String> errors = errors("stats:\n  str: {short-name: Quick}\n  spd: {aliases: [quick]}\n");
        assertEquals(List.of("stats.str.short-name: 'Quick' is already a name of stat 'spd'"
                + " (commands accept short names)"), errors);
    }

    @Test
    void aShortNameWithASpaceOrMaxPrefixIsOnlyDisplayText() {
        StatDefinitions stats = load("stats:\n  temphp: {short-name: \"Temp HP\"}\n  luck: {short-name: max_l}\n"
                + "  dex: {short-name: \"temp\"}\n").stats();
        assertEquals("Temp HP", stats.get("temphp").orElseThrow().display().shortName());
        assertEquals(Optional.empty(), stats.get("temphp").orElseThrow().display().shortNameInput());
        assertEquals(Optional.empty(), stats.get("luck").orElseThrow().display().shortNameInput());
        assertTrue(stats.find("max_l").isEmpty());
        assertEquals("dex", stats.find("temp").orElseThrow().id());
    }

    @Test
    void theOldConstructorHasNoCustomShortName() {
        StatDisplay display = new StatDisplay("Luck", "<green>", List.of("fortune"));
        assertEquals("Luck", display.shortName());
        assertFalse(display.customShortName());
        assertEquals(List.of("fortune"), display.aliases());
    }
}
