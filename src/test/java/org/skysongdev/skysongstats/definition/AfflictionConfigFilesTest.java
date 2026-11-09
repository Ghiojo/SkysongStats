package org.skysongdev.skysongstats.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.definition.AfflictionTestFiles.STATS;

/**
 * Reading plugins/SkysongStats/conditions.yml and injuries.yml.
 */
class AfflictionConfigFilesTest {
    private static final String CONDITIONS = "conditions:\n  bleeding: {name: Bleeding}\n";
    private static final String INJURIES = "severities: [minor]\ninjuries:\n  cut: {severity: minor, "
            + "effects: {conditions: [bleeding]}}\n";

    @TempDir
    Path folder;

    private ConditionDefinitions conditions(File file) {
        return ConditionsConfigFile.load(file, CONDITIONS, STATS, name -> true);
    }

    @Test
    void theFirstStartWritesBothBundledFiles() throws IOException {
        File conditionsFile = folder.resolve("plugin/conditions.yml").toFile();
        File injuriesFile = folder.resolve("plugin/injuries.yml").toFile();
        ConditionDefinitions conditions = conditions(conditionsFile);
        InjuryDefinitions injuries = InjuriesConfigFile.load(injuriesFile, INJURIES, STATS, conditions, name -> true);
        assertEquals(CONDITIONS, Files.readString(conditionsFile.toPath(), StandardCharsets.UTF_8));
        assertEquals(INJURIES, Files.readString(injuriesFile.toPath(), StandardCharsets.UTF_8));
        assertEquals("Bleeding", conditions.get("bleeding").orElseThrow().name());
        assertEquals(1, injuries.get("cut").orElseThrow().conditions().get("bleeding"));
    }

    @Test
    void theServersFileIsUsedAndNotOverwritten() throws IOException {
        File file = folder.resolve("conditions.yml").toFile();
        String edited = CONDITIONS + "  tired: {}\n";
        Files.writeString(file.toPath(), edited);
        assertTrue(conditions(file).get("tired").isPresent());
        assertEquals(edited, Files.readString(file.toPath()));
    }

    @Test
    void mistakesNameTheirFile() throws IOException {
        File conditionsFile = folder.resolve("conditions.yml").toFile();
        Files.writeString(conditionsFile.toPath(), "conditions:\n  deep_cut: {}\n");
        DefinitionException conditionError = assertThrows(DefinitionException.class, () -> conditions(conditionsFile));
        assertTrue(conditionError.getMessage().startsWith("conditions.yml has 1 error(s)"), conditionError.getMessage());

        File injuriesFile = folder.resolve("injuries.yml").toFile();
        Files.writeString(injuriesFile.toPath(), INJURIES.replace("bleeding", "poisoned"));
        ConditionDefinitions loaded = ConditionsConfigLoader.load(AfflictionTestFiles.yaml(CONDITIONS), STATS, name -> true);
        DefinitionException injuryError = assertThrows(DefinitionException.class,
                () -> InjuriesConfigFile.load(injuriesFile, INJURIES, STATS, loaded, name -> true));
        assertTrue(injuryError.getMessage().startsWith("injuries.yml has 1 error(s)"), injuryError.getMessage());
    }
}
