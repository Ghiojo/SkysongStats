package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the ancestries.yml shipped in the jar loads against the shipped
 * stats.yml, with one example for each way an ancestry can adjust stats.
 */
class DefaultAncestriesFileTest {

    private static YamlConfiguration bundled(String name) throws IOException {
        try (InputStream in = DefaultAncestriesFileTest.class.getClassLoader().getResourceAsStream(name);
             Reader reader = new InputStreamReader(in, UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private static AncestryDefinitions shipped() throws IOException {
        StatDefinitions stats = StatsConfigLoader.load(bundled("stats.yml"), name -> true).stats();
        return AncestriesConfigLoader.load(bundled("ancestries.yml"), stats, name -> true);
    }

    @Test
    void theDefaultFileLoadsWithItsExamples() throws IOException {
        assertEquals(List.of("dwarf", "elf", "human", "goblin", "orc", "giant", "dragonborn"),
                shipped().all().stream().map(AncestryDefinition::id).toList());
    }

    @Test
    void theExamplesShowEveryFeature() throws IOException {
        AncestryDefinitions ancestries = shipped();
        AncestryDefinition dwarf = ancestries.get("dwarf").orElseThrow();
        assertEquals(List.of(new AncestryAdjustment("con", 1), new AncestryAdjustment("spd", -1)), dwarf.adjustments());
        assertFalse(dwarf.hasChoices());

        StatPickChoice elf = (StatPickChoice) ancestries.get("elf").orElseThrow().choices().getFirst();
        assertEquals(List.of("dex", "foc"), elf.from());

        StatPickChoice human = (StatPickChoice) ancestries.get("human").orElseThrow().choices().getFirst();
        assertTrue(human.anyStat());
        assertEquals(2, human.picks());

        StatPickChoice goblin = (StatPickChoice) ancestries.get("goblin").orElseThrow().choices().getFirst();
        assertFalse(goblin.isBoost());

        PackageChoice orc = (PackageChoice) ancestries.get("orc").orElseThrow().choices().getFirst();
        assertEquals(2, orc.options().size());

        assertTrue(ancestries.get("giant").orElseThrow().choices().getFirst().stack());
        assertEquals("skysong.ancestry.dragonborn", ancestries.get("dragonborn").orElseThrow().permission().orElseThrow());
    }
}
