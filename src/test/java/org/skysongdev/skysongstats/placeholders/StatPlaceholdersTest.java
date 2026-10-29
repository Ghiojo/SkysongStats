package org.skysongdev.skysongstats.placeholders;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StoredModifier;
import org.skysongdev.skysongstats.storage.StoredStats;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated stat placeholders, against the default stats.yml.
 */
class StatPlaceholdersTest {

    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");

    // Nothing needs to be saved for these tests.
    private static final StatRepository NO_STORAGE = new StatRepository() {
        @Override
        public void createTables() {
        }

        @Override
        public Map<ProfileKey, StoredStats> loadAll() {
            return Map.of();
        }

        @Override
        public void saveValue(ProfileKey character, String statId, int value) {
        }

        @Override
        public void saveModifier(ProfileKey character, StoredModifier modifier) {
        }

        @Override
        public void deleteModifier(String modifierId) {
        }

        @Override
        public void deleteCharacter(ProfileKey character) {
        }
    };

    private StatService service;
    private StatPlaceholders placeholders;

    private static StatDefinitions defaultStats() throws Exception {
        try (InputStream in = StatPlaceholdersTest.class.getClassLoader().getResourceAsStream("stats.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return StatsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> true).stats();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new StatService(defaultStats(), NO_STORAGE, change -> { });
        service.createCharacter(ALICE);
        placeholders = new StatPlaceholders(service);
        service.apply(ALICE, service.definitions().get("con").orElseThrow(), StatAction.SET, 2);
        service.addModifier(ALICE, "con", 1);
        service.addModifier(ALICE, "max_hp", 5);
        service.apply(ALICE, service.definitions().get("hp").orElseThrow(), StatAction.REDUCE, 20);
        service.setItemCounts(ALICE, Map.of(new ItemTagSource("arcrot", false), 4));
    }

    private String resolve(String placeholder) {
        return placeholders.resolve(ALICE, placeholder).orElseThrow(() -> new AssertionError(placeholder));
    }

    @ParameterizedTest
    @CsvSource({
            "con, 3",
            "con_base, 2",
            "con_modifiers, 1",
            "max_hp, 95",
            "max_hp_modifiers, 5",
            "hp, 40",
            "hp_base, 40",
            "hp_modifiers, 0",
            "spd, 6",
            "arcrot, 4",
            "arcrot_base, 0",
            "max_arcrot, 35",
            "MAX_HP, 95"
    })
    void resolvesEveryGeneratedPlaceholder(String placeholder, String expected) {
        assertEquals(expected, resolve(placeholder), placeholder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"luck", "luck_base", "luck_modifiers", "max_str", "max_str_modifiers", "max_hp_base",
            "strength", "character_name", ""})
    void otherNamesAreNotStatPlaceholders(String placeholder) {
        assertEquals(Optional.empty(), placeholders.resolve(ALICE, placeholder), placeholder);
    }

    @Test
    void charactersWithoutStatsHaveNoValues() {
        assertTrue(placeholders.resolve(new ProfileKey(UUID.randomUUID(), "main"), "hp").isEmpty());
    }
}
