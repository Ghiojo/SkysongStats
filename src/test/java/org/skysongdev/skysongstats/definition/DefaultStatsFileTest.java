package org.skysongdev.skysongstats.definition;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.definition.StatDefinitions.CommandBinding;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the stats.yml shipped in the jar loads without errors and describes
 * the plugin as it works today. MockBukkit is only needed for the Material check.
 */
class DefaultStatsFileTest {

    private static StatsConfig config;

    @BeforeAll
    static void loadDefaultFile() throws Exception {
        MockBukkit.mock();
        try (InputStream in = DefaultStatsFileTest.class.getClassLoader().getResourceAsStream("stats.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            config = StatsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> {
                Material material = Material.matchMaterial(name);
                return material != null && material.isItem();
            });
        }
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    private static StatDefinition stat(String id) {
        return config.stats().get(id).orElseThrow(() -> new AssertionError("no stat " + id));
    }

    private static int max(String bar, int con, int foc) {
        return stat(bar).bar().max().evaluate(Map.of("con", (double) con, "foc", (double) foc)::get);
    }

    @Test
    void hasTodaysStats() {
        assertEquals(List.of("str", "dex", "con", "foc", "spd", "hp", "temphp", "ac", "potion", "arcrot", "fictis"),
                config.stats().all().stream().map(StatDefinition::id).toList());
    }

    @Test
    void setupRulesAreTodays() {
        assertEquals(new SetupRules(7, 3), config.setupRules());
    }

    @Test
    void attributesShowInSetupWithTodaysLimitsIconsAndSlots() {
        List<StatDefinition> setup = config.stats().setupStats();
        assertEquals(List.of("str", "dex", "con", "foc", "spd"), setup.stream().map(StatDefinition::id).toList());
        assertEquals(List.of(9, 11, 13, 15, 17), setup.stream().map(stat -> stat.setup().slot()).toList());
        assertEquals(List.of("IRON_SWORD", "BOW", "APPLE", "ENDER_EYE", "FEATHER"),
                setup.stream().map(stat -> stat.setup().icon().material()).toList());
        for (StatDefinition stat : setup) {
            assertEquals(-3, stat.setup().min());
            assertEquals(4, stat.setup().max());
            assertFalse(stat.setup().lore().isEmpty());
        }
    }

    @Test
    void baselinesAreZeroExceptSpeed() {
        for (String id : List.of("str", "dex", "con", "foc", "temphp", "ac")) {
            assertEquals(0, stat(id).baseline(), id);
        }
        assertEquals(6, stat("spd").baseline());
    }

    @Test
    void longNamesAreAliases() {
        assertEquals("str", config.stats().find("strength").orElseThrow().id());
        assertEquals("dex", config.stats().find("Dexterity").orElseThrow().id());
        assertEquals("con", config.stats().find("constitution").orElseThrow().id());
        assertEquals("foc", config.stats().find("focus").orElseThrow().id());
        assertEquals("spd", config.stats().find("speed").orElseThrow().id());
    }

    @Test
    void barMaximumsMatchTodaysHardcodedFormulas() {
        for (int value = -3; value <= 4; value++) {
            assertEquals(60 + 10 * value, max("hp", value, 0));
            assertEquals(9 + value, max("potion", value, 0));
            assertEquals(35 + 2 * value, max("arcrot", 0, value));
            assertEquals(10 + value, max("fictis", 0, value));
        }
    }

    @Test
    void barsStartLikeToday() {
        assertTrue(stat("hp").bar().startAtMax());
        for (String id : List.of("potion", "arcrot", "fictis")) {
            assertFalse(stat(id).bar().startAtMax(), id);
            assertEquals(0, stat(id).bar().start(), id);
            assertEquals(0, stat(id).minimum().orElseThrow(), id);
        }
    }

    @Test
    void tempHpShieldsHpFromDamage() {
        assertEquals("temphp", stat("hp").bar().shield());
        assertEquals(0, stat("temphp").minimum().orElseThrow());
        assertEquals(0, stat("ac").minimum().orElseThrow());
    }

    @Test
    void todaysCommandsComeFromTheFile() {
        Map<String, String> expected = Map.of(
                "heal", "hp add", "addhp", "hp add",
                "reducehp", "hp reduce", "rhp", "hp reduce",
                "damage", "hp damage", "dhp", "hp damage",
                "temphp", "temphp set", "ac", "ac set", "potionsickness", "potion set");
        assertEquals(expected.size(), config.stats().commands().size());
        expected.forEach((label, target) -> {
            CommandBinding binding = config.stats().findCommand(label).orElseThrow(() -> new AssertionError(label));
            assertEquals(target, binding.stat().id() + " " + binding.action().configName(), label);
        });
    }

    @Test
    void arcrotAndFictisCountTheirSkysongItemTags() {
        assertEquals(List.of(new ItemTagSource("arcrot", false), new ItemTagSource("charged_arcrot", true)),
                stat("arcrot").itemTags());
        assertEquals(List.of(new ItemTagSource("fictis", false)), stat("fictis").itemTags());
    }
}
