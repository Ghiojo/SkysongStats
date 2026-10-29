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
 * Reading plugins/SkysongStats/skills.yml.
 */
class SkillsConfigFileTest {
    private static final String BUNDLED = """
            levels:
              unskilled: {xp: 0}
            skills:
              fishing: {name: Fishing}
            """;

    @TempDir
    Path folder;

    private SkillsConfig load(File file) {
        return SkillsConfigFile.load(file, BUNDLED, name -> true);
    }

    @Test
    void theFirstStartWritesTheBundledFile() throws IOException {
        File file = folder.resolve("plugin/skills.yml").toFile();
        SkillsConfig config = load(file);
        assertEquals(BUNDLED, Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertEquals("Fishing", config.skills().get("fishing").orElseThrow().name());
    }

    @Test
    void theServersFileIsUsedAndNotOverwritten() throws IOException {
        File file = folder.resolve("skills.yml").toFile();
        String edited = BUNDLED + "  cooking: {baseline: 2}\n";
        Files.writeString(file.toPath(), edited);
        SkillsConfig config = load(file);
        assertEquals(2, config.skills().get("cooking").orElseThrow().baseline());
        assertEquals(edited, Files.readString(file.toPath()));
    }

    @Test
    void mistakesAreReportedForSkillsYml() throws IOException {
        File file = folder.resolve("skills.yml").toFile();
        Files.writeString(file.toPath(), "skills: {}\n");
        DefinitionException error = assertThrows(DefinitionException.class, () -> load(file));
        assertTrue(error.getMessage().startsWith("skills.yml has 1 error(s)"), error.getMessage());
    }

    @Test
    void aBrokenYamlFileIsReported() throws IOException {
        File file = folder.resolve("skills.yml").toFile();
        Files.writeString(file.toPath(), "skills: [unclosed\n");
        DefinitionException error = assertThrows(DefinitionException.class, () -> load(file));
        assertTrue(error.getMessage().contains("skills.yml has 1 error(s):\n - could not be read"), error.getMessage());
    }

    @Test
    void theJarsDefaultFileLoads() throws IOException {
        File file = folder.resolve("skills.yml").toFile();
        String bundled;
        try (var in = SkillsConfigFileTest.class.getClassLoader().getResourceAsStream("skills.yml")) {
            bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(18, SkillsConfigFile.load(file, bundled, name -> true).skills().all().size());
    }
}
