package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading plugins/SkysongStats/proficiencies.yml.
 */
class ProficienciesConfigFileTest {
    private static final SkillDefinitions SKILLS = new SkillDefinitions(List.of(
            new SkillDefinition("martial", "Martial", "Martial", "", List.of(), 0, null)));
    private static final String BUNDLED = """
            cost: 4
            tiers:
              untrained: {}
              proficient: {}
            proficiencies:
              axes: {name: Axes, skill: martial}
            """;

    @TempDir
    Path folder;

    private ProficienciesConfig load(File file) {
        return ProficienciesConfigFile.load(file, BUNDLED, SKILLS);
    }

    @Test
    void theFirstStartWritesTheBundledFile() throws IOException {
        File file = folder.resolve("plugin/proficiencies.yml").toFile();
        ProficienciesConfig config = load(file);
        assertEquals(BUNDLED, Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertEquals("Axes", config.proficiencies().get("axes").orElseThrow().name());
    }

    @Test
    void theServersFileIsUsedAndNotOverwritten() throws IOException {
        File file = folder.resolve("proficiencies.yml").toFile();
        String edited = BUNDLED + "  bows: {skill: martial, cost: 7}\n";
        Files.writeString(file.toPath(), edited);
        ProficienciesConfig config = load(file);
        assertEquals(7, config.proficiencies().get("bows").orElseThrow().cost());
        assertEquals(edited, Files.readString(file.toPath()));
    }

    @Test
    void mistakesAreReportedForProficienciesYml() throws IOException {
        File file = folder.resolve("proficiencies.yml").toFile();
        Files.writeString(file.toPath(), BUNDLED.replace("martial", "sailing"));
        DefinitionException error = assertThrows(DefinitionException.class, () -> load(file));
        assertTrue(error.getMessage().startsWith("proficiencies.yml has 1 error(s)"), error.getMessage());
    }

    @Test
    void aBrokenYamlFileIsReported() throws IOException {
        File file = folder.resolve("proficiencies.yml").toFile();
        Files.writeString(file.toPath(), "proficiencies: [unclosed\n");
        DefinitionException error = assertThrows(DefinitionException.class, () -> load(file));
        assertTrue(error.getMessage().contains("proficiencies.yml has 1 error(s):\n - could not be read"), error.getMessage());
    }
}
