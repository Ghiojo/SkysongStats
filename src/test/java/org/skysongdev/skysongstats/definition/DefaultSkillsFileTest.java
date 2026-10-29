package org.skysongdev.skysongstats.definition;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the skills.yml shipped in the jar loads without errors and describes
 * the skills as the plugin has them hardcoded today (Utils.Skills, SkillManager,
 * SkillChoiceGUI, ViewSkills). MockBukkit is only needed for the Material check.
 */
class DefaultSkillsFileTest {

    private static SkillsConfig config;

    // id -> name, book colour, setup icon, setup slot, as hardcoded today.
    private static final Map<String, List<Object>> TODAY = new LinkedHashMap<>();

    static {
        TODAY.put("animalhandling", List.of("Animal Handling", "<#45c2ea>", "BONE", 10));
        TODAY.put("farming", List.of("Farming", "<#cdaf22>", "WHEAT", 11));
        TODAY.put("forestry", List.of("Forestry", "<#54bf3e>", "FERN", 12));
        TODAY.put("mining", List.of("Mining", "<#a0836f>", "IRON_ORE", 19));
        TODAY.put("mistgathering", List.of("Mist Gathering", "<gold>", "GLOWSTONE_DUST", 20));
        TODAY.put("woodprocessing", List.of("Wood Processing", "<#ba7f3d>", "OAK_LOG", 21));
        TODAY.put("alchemy", List.of("Alchemy", "<#aaeb38>", "BREWING_STAND", 37));
        TODAY.put("artificing", List.of("Artificing", "<#2cf0f1>", "HOST_ARMOR_TRIM_SMITHING_TEMPLATE", 38));
        TODAY.put("cooking", List.of("Cooking", "<#f77b3c>", "COOKED_BEEF", 39));
        TODAY.put("craftsman", List.of("Craftsman", "<#eba751>", "ANVIL", 40));
        TODAY.put("metalworking", List.of("Metalworking", "<#8d8d8d>", "IRON_INGOT", 41));
        TODAY.put("tailoring", List.of("Tailoring", "<#ea5be7>", "FLOWER_BANNER_PATTERN", 42));
        TODAY.put("witchcraft", List.of("Witchcraft", "<#8235b0>", "ENCHANTING_TABLE", 43));
        TODAY.put("economical", List.of("Economical", "<gold>", "GOLD_NUGGET", 14));
        TODAY.put("entertainment", List.of("Entertainment", "<#f22a93>", "MUSIC_DISC_CAT", 15));
        TODAY.put("martial", List.of("Martial", "<#e18048>", "IRON_SWORD", 16));
        TODAY.put("medical", List.of("Medical", "<#f14f4f>", "PAPER", 23));
        TODAY.put("tande", List.of("Transportation and Exploration", "<#7fe95b>", "COMPASS", 24));
    }

    // id -> every name today's /skills addxp and setxp accept for it, in any check.
    private static final Map<String, List<String>> TODAYS_NAMES = Map.ofEntries(
            Map.entry("animalhandling", List.of("animalhandling", "ah")),
            Map.entry("farming", List.of("farming", "farm")),
            Map.entry("forestry", List.of("forestry", "forest", "for")),
            Map.entry("mining", List.of("mining", "min")),
            Map.entry("mistgathering", List.of("mistgathering", "mist")),
            Map.entry("woodprocessing", List.of("woodprocessing", "wood")),
            Map.entry("alchemy", List.of("alchemy", "alch")),
            Map.entry("artificing", List.of("artificing", "arti")),
            Map.entry("cooking", List.of("cooking", "cook")),
            Map.entry("craftsman", List.of("craftsman", "craft")),
            Map.entry("metalworking", List.of("metalworking", "metal")),
            Map.entry("tailoring", List.of("tailoring", "tailor")),
            Map.entry("witchcraft", List.of("witchcraft", "witch")),
            Map.entry("economical", List.of("economical", "eco")),
            Map.entry("entertainment", List.of("entertainment", "ent")),
            Map.entry("martial", List.of("martial")),
            Map.entry("medical", List.of("medical", "med", "medic")),
            Map.entry("tande", List.of("tande", "transportationandexploration", "t&e", "T&E")));

    @BeforeAll
    static void loadDefaultFile() throws Exception {
        MockBukkit.mock();
        try (InputStream in = DefaultSkillsFileTest.class.getClassLoader().getResourceAsStream("skills.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            config = SkillsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> {
                Material material = Material.matchMaterial(name);
                return material != null && material.isItem();
            });
        }
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    private static SkillDefinition skill(String id) {
        return config.skills().get(id).orElseThrow(() -> new AssertionError("no skill " + id));
    }

    @Test
    void hasTodaysEighteenSkillsWithTheirIds() {
        assertEquals(List.copyOf(TODAY.keySet()), config.skills().all().stream().map(SkillDefinition::id).toList());
    }

    @Test
    void namesColoursIconsAndSlotsAreTodays() {
        for (Map.Entry<String, List<Object>> today : TODAY.entrySet()) {
            String id = today.getKey();
            SkillDefinition skill = skill(id);
            assertEquals(today.getValue().get(0), skill.name(), id);
            assertEquals(today.getValue().get(1), skill.color(), id);
            SkillSetup setup = skill.setupOptions().orElseThrow();
            assertEquals(today.getValue().get(2), setup.icon().material(), id);
            assertEquals(today.getValue().get(3), setup.slot(), id);
            assertFalse(setup.lore().isEmpty(), id);
        }
    }

    @Test
    void theBookShowsTandEAndTheSetupScreenItsFullName() {
        assertEquals("T&E", skill("tande").shortName());
        assertEquals("Farming", skill("farming").shortName());
    }

    @Test
    void onlyFarmingHasABrighterColourOnTheSetupScreen() {
        for (SkillDefinition skill : config.skills().all()) {
            String expected = "farming".equals(skill.id()) ? "<#f9f36d>" : "";
            assertEquals(expected, skill.setup().color(), skill.id());
        }
    }

    @Test
    void everyNameTodaysCommandsAcceptFindsItsSkill() {
        for (Map.Entry<String, List<String>> skill : TODAYS_NAMES.entrySet()) {
            for (String name : skill.getValue()) {
                assertEquals(skill.getKey(), config.skills().find(name).map(SkillDefinition::id).orElse(null), name);
            }
        }
    }

    @Test
    void levelsAreTodaysThresholdsNamesAndColours() {
        assertEquals(List.of(
                new SkillLevel("unskilled", "Unskilled", "<dark_gray>", 0),
                new SkillLevel("novice", "Novice", "<#6cda33>", 16),
                new SkillLevel("competent", "Competent", "<blue>", 40),
                new SkillLevel("journeyman", "Journeyman", "<#cc6ce9>", 80),
                new SkillLevel("expert", "Expert", "<gold>", 128)), config.levels().all());
        assertEquals("unskilled", config.levels().levelFor(15).id());
        assertEquals("journeyman", config.levels().levelFor(127).id());
        assertEquals("expert", config.levels().levelFor(128).id());
    }

    @Test
    void setupPicksTwoSkillsAtNovice() {
        assertEquals(2, config.setup().picks());
        assertEquals(16, config.setup().pickedLevel().xp());
    }

    @Test
    void newCharactersStartWithNoXp() {
        for (SkillDefinition skill : config.skills().all()) {
            assertEquals(0, skill.baseline(), skill.id());
        }
    }

    @Test
    void theBookHasTodaysThreePages() {
        assertEquals(List.of("gathering", "crafting", "service"), config.book().stream().map(SkillBookPage::id).toList());
        assertEquals(List.of("animalhandling", "farming", "forestry", "mining", "mistgathering", "woodprocessing"),
                config.book().get(0).skills());
        assertEquals(List.of("alchemy", "artificing", "cooking", "craftsman", "metalworking", "tailoring", "witchcraft"),
                config.book().get(1).skills());
        assertEquals(List.of("economical", "entertainment", "martial", "medical", "tande"), config.book().get(2).skills());
        assertEquals("<green>Gathering Skills:</green>", config.book().get(0).title());
        assertTrue(config.skillsOnNoPage().isEmpty());
    }
}
