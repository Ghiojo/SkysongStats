package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading plugins/SkysongStats/stats.yml.
 */
class StatsConfigFileTest {
    private static final String BUNDLED = """
            setup:
              points: 7
              negative-points: 3
            stats:
              str: {name: Strength}
            """;

    @TempDir
    Path folder;

    private StatsConfig load(File file) {
        return StatsConfigFile.load(file, BUNDLED, name -> true);
    }

    @Test
    void theFirstStartWritesTheBundledFile() throws IOException {
        File file = folder.resolve("plugin/stats.yml").toFile();
        StatsConfig config = load(file);
        assertEquals(BUNDLED, Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertEquals("Strength", config.stats().get("str").orElseThrow().display().name());
        assertEquals(7, config.setupRules().points());
    }

    @Test
    void theServersFileIsUsedAndNotOverwritten() throws IOException {
        File file = folder.resolve("stats.yml").toFile();
        String edited = BUNDLED + "  luck: {baseline: 2}\n";
        Files.writeString(file.toPath(), edited);
        StatsConfig config = load(file);
        assertEquals(2, config.stats().get("luck").orElseThrow().baseline());
        assertEquals(edited, Files.readString(file.toPath()));
    }

    @Test
    void mistakesAreAllReported() throws IOException {
        File file = folder.resolve("stats.yml").toFile();
        Files.writeString(file.toPath(), "stats:\n  str: {type: nonsense}\n  Bad Id: {}\n");
        DefinitionException error = assertThrows(DefinitionException.class, () -> load(file));
        assertTrue(error.getErrors().size() >= 2, error.getErrors().toString());
    }

    @Test
    void brokenYamlIsReported() throws IOException {
        File file = folder.resolve("stats.yml").toFile();
        Files.writeString(file.toPath(), "stats: [unclosed\n");
        DefinitionException error = assertThrows(DefinitionException.class, () -> load(file));
        assertTrue(error.getErrors().get(0).startsWith("could not be read"), error.getErrors().toString());
    }

    @Test
    void theDefaultFileLoadsWithRealMaterials() {
        File file = folder.resolve("stats.yml").toFile();
        String bundled = new String(readResource(), StandardCharsets.UTF_8);
        assertTrue(StatsConfigFile.load(file, bundled).stats().get("hp").isPresent());
    }

    private static byte[] readResource() {
        try (var in = StatsConfigFileTest.class.getClassLoader().getResourceAsStream("stats.yml")) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
