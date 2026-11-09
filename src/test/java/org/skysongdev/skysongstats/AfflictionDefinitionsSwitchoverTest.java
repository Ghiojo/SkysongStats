package org.skysongdev.skysongstats;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The plugin loads conditions.yml and injuries.yml on start and on /ssreload. A
 * file with mistakes is not reloaded and keeps its definitions from before.
 */
class AfflictionDefinitionsSwitchoverTest {
    private SkysongStats plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void write(String file, String text) throws IOException {
        Files.writeString(new File(plugin.getDataFolder(), file).toPath(), text, StandardCharsets.UTF_8);
    }

    @Test
    void theFirstStartWritesAndLoadsTheDefaultFiles() {
        assertTrue(new File(plugin.getDataFolder(), "conditions.yml").isFile());
        assertTrue(new File(plugin.getDataFolder(), "injuries.yml").isFile());
        assertEquals(List.of(), plugin.getConditionDefinitions().all());
        assertEquals(List.of("minor", "moderate", "severe", "permanent"), plugin.getInjuryDefinitions().severities());
    }

    @Test
    void ssreloadLoadsChangedFiles() throws IOException {
        write("conditions.yml", "conditions:\n  bleeding: {effects: {modifiers: \"con-$\"}}\n");
        write("injuries.yml", "severities: [minor]\ninjuries:\n  cut: {severity: minor, effects: {conditions: [bleeding]}}\n");

        assertEquals(List.of(), plugin.reloadSettings());

        assertTrue(plugin.getConditionDefinitions().get("bleeding").isPresent());
        assertEquals(1, plugin.getInjuryDefinitions().get("cut").orElseThrow().conditions().get("bleeding"));
    }

    @Test
    void aFileWithMistakesKeepsItsDefinitionsFromBefore() throws IOException {
        write("conditions.yml", "conditions:\n  bleeding: {}\n");
        write("injuries.yml", "severities: [minor]\ninjuries:\n  cut: {severity: minor}\n");
        plugin.reloadSettings();
        write("conditions.yml", "conditions:\n  deep_cut: {}\n");
        write("injuries.yml", "severities: [minor]\ninjuries:\n  cut: {severity: deadly}\n");

        assertEquals(List.of("conditions.yml", "injuries.yml"), plugin.reloadSettings());

        assertTrue(plugin.getConditionDefinitions().get("bleeding").isPresent());
        assertEquals("minor", plugin.getInjuryDefinitions().get("cut").orElseThrow().severity());
    }

    @Test
    void injuriesAreCheckedAgainstTheConditionsKeptFromBefore() throws IOException {
        write("conditions.yml", "conditions:\n  bleeding: {}\n");
        write("injuries.yml", "severities: [minor]\ninjuries: {}\n");
        plugin.reloadSettings();
        write("conditions.yml", "conditions:\n  bleeding: {}\n  dizzy_2: {}\n");
        write("injuries.yml", "severities: [minor]\ninjuries:\n  cut: {severity: minor, effects: {conditions: [bleeding]}}\n");

        assertEquals(List.of("conditions.yml"), plugin.reloadSettings());

        assertTrue(plugin.getInjuryDefinitions().get("cut").isPresent());
    }
}
