package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.yaml;

/**
 * Reading plugins/SkysongStats/ancestries.yml.
 */
class AncestriesConfigFileTest {
    private static final StatDefinitions STATS = StatsConfigLoader.load(
            yaml("stats:\n  con: {setup: {icon: APPLE, slot: 13, min: -3, max: 4}}\n"), name -> true).stats();
    private static final String ANCESTRIES = "ancestries:\n  dwarf: {icon: IRON_PICKAXE, slot: 10, stats: {con: 1}}\n";

    @TempDir
    Path folder;

    private AncestryDefinitions load(File file) {
        return AncestriesConfigFile.load(file, ANCESTRIES, STATS, name -> true);
    }

    @Test
    void theFirstStartWritesTheBundledFile() throws IOException {
        File file = folder.resolve("plugin/ancestries.yml").toFile();
        AncestryDefinitions ancestries = load(file);
        assertEquals(ANCESTRIES, Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertEquals(1, ancestries.get("dwarf").orElseThrow().fixedAmount("con"));
    }

    @Test
    void theServersFileIsUsedAndNotOverwritten() throws IOException {
        File file = folder.resolve("ancestries.yml").toFile();
        String edited = "ancestries: {}\n";
        Files.writeString(file.toPath(), edited);
        assertTrue(load(file).isEmpty());
        assertEquals(edited, Files.readString(file.toPath()));
    }

    @Test
    void mistakesNameTheFile() throws IOException {
        File file = folder.resolve("ancestries.yml").toFile();
        Files.writeString(file.toPath(), "ancestries:\n  dwarf: {icon: STONE, slot: 10, stats: {str: 1}}\n");
        DefinitionException error = assertThrows(DefinitionException.class, () -> load(file));
        assertTrue(error.getMessage().startsWith("ancestries.yml has 1 error(s)"), error.getMessage());
    }
}
