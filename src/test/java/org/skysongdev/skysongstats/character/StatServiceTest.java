package org.skysongdev.skysongstats.character;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link StatService} and {@link StatCalculator}, run
 * against the default stats.yml and an in-memory repository.
 */
class StatServiceTest {

    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");

    // Keeps saved data in maps, like the database would.
    private static final class MemoryRepository implements StatRepository {
        final Map<ProfileKey, Map<String, Integer>> values = new HashMap<>();
        final Map<ProfileKey, List<StoredModifier>> modifiers = new HashMap<>();

        @Override
        public void createTables() {
        }

        @Override
        public Map<ProfileKey, StoredStats> loadAll() {
            Map<ProfileKey, StoredStats> all = new LinkedHashMap<>();
            values.forEach((key, saved) -> all.put(key,
                    new StoredStats(saved, modifiers.getOrDefault(key, List.of()))));
            return all;
        }

        @Override
        public void saveValue(ProfileKey character, String statId, int value) {
            values.computeIfAbsent(character, k -> new HashMap<>()).put(statId, value);
        }

        @Override
        public void saveModifier(ProfileKey character, StoredModifier modifier) {
            modifiers.computeIfAbsent(character, k -> new ArrayList<>()).add(modifier);
        }

        @Override
        public void deleteModifier(String modifierId) {
            modifiers.values().forEach(list -> list.removeIf(modifier -> modifier.id().equals(modifierId)));
        }

        @Override
        public void deleteCharacter(ProfileKey character) {
            values.remove(character);
            modifiers.remove(character);
        }
    }

    private static StatDefinitions defaultStats;

    private MemoryRepository repository;
    private List<StatChange> changes;
    private StatService service;

    private static StatDefinitions loadDefaultStats() throws Exception {
        try (InputStream in = StatServiceTest.class.getClassLoader().getResourceAsStream("stats.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return StatsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> true).stats();
        }
    }

    private static StatDefinitions stats(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return StatsConfigLoader.load(config, name -> true).stats();
    }

    @BeforeEach
    void setUp() throws Exception {
        if (defaultStats == null) {
            defaultStats = loadDefaultStats();
        }
        repository = new MemoryRepository();
        changes = new ArrayList<>();
        service = new StatService(defaultStats, repository, changes::add);
        service.createCharacter(ALICE);
        changes.clear();
    }

    private StatDefinition stat(String id) {
        return service.definitions().get(id).orElseThrow();
    }

    private int value(String name) {
        return service.value(ALICE, name).orElseThrow(() -> new AssertionError("no value " + name));
    }

    private StatChange apply(String stat, StatAction action, int amount) {
        return service.apply(ALICE, stat(stat), action, amount);
    }

    @Test
    void newCharactersStartAtTheirBaselinesAndFullHp() {
        assertEquals(0, value("str"));
        assertEquals(6, value("spd"));
        assertEquals(60, value("max_hp"));
        assertEquals(60, value("hp"));
        assertEquals(0, value("temphp"));
        assertEquals(0, value("potion"));
        assertEquals(9, value("max_potion"));
        assertEquals(35, value("max_arcrot"));
        assertEquals(10, value("max_fictis"));
    }

    @Test
    void ensureCharacterKeepsAnExistingCharacter() {
        service.apply(ALICE, stat("str"), StatAction.SET, 3);
        service.ensureCharacter(ALICE);
        assertEquals(3, value("str"));
    }

    @Test
    void ensureCharacterCreatesAMissingCharacterAtItsStartValues() {
        ProfileKey older = new ProfileKey(UUID.randomUUID(), "Default");
        service.ensureCharacter(older);
        assertEquals(6, service.value(older, "spd").orElseThrow());
        assertEquals(6, repository.values.get(older).get("spd"));
    }

    @Test
    void everyStatIsSavedAtItsStartValueOnCreation() {
        Map<String, Integer> expected = new HashMap<>(Map.of("str", 0, "dex", 0, "con", 0, "foc", 0, "spd", 6,
                "hp", 60, "temphp", 0, "ac", 0, "potion", 0, "arcrot", 0));
        expected.put("fictis", 0);
        assertEquals(expected, repository.values.get(ALICE));
    }

    @Test
    void unknownNamesHaveNoValue() {
        assertTrue(service.value(ALICE, "luck").isEmpty());
        assertTrue(service.value(ALICE, "max_str").isEmpty());
        assertTrue(service.value(new ProfileKey(UUID.randomUUID(), "main"), "str").isEmpty());
    }

    // Bug 7: "/stats set str 3" must set the stat.
    @Test
    void setChangesTheBaseValueAndIsSaved() {
        StatChange change = apply("str", StatAction.SET, 3);
        assertEquals(new StatChange(ALICE, stat("str"), 0, 3), change);
        assertEquals(3, value("str"));
        assertEquals(3, repository.values.get(ALICE).get("str"));
        assertEquals(List.of(change), changes);
    }

    @Test
    void setCanBeNegativeForStatsWithoutAFloor() {
        apply("str", StatAction.SET, -3);
        assertEquals(-3, value("str"));
    }

    @Test
    void addAndReduceChangeTheBase() {
        apply("spd", StatAction.ADD, 2);
        assertEquals(8, value("spd"));
        apply("spd", StatAction.REDUCE, 5);
        assertEquals(3, value("spd"));
    }

    @Test
    void negativeAmountsAreRefusedExceptForSet() {
        assertThrows(IllegalArgumentException.class, () -> apply("hp", StatAction.ADD, -1));
        assertThrows(IllegalArgumentException.class, () -> apply("hp", StatAction.REDUCE, -1));
        assertThrows(IllegalArgumentException.class, () -> apply("hp", StatAction.DAMAGE, -1));
    }

    @Test
    void constitutionRaisesMaxHpThroughTheFormula() {
        apply("con", StatAction.SET, 2);
        assertEquals(80, value("max_hp"));
        assertEquals(11, value("max_potion"));
        apply("foc", StatAction.SET, 3);
        assertEquals(41, value("max_arcrot"));
        assertEquals(13, value("max_fictis"));
    }

    @Test
    void hpStopsAtItsMaxAndAtZero() {
        assertEquals(60, apply("hp", StatAction.ADD, 25).newValue());
        // Bug 15: /damage could take HP below 0.
        assertEquals(0, apply("hp", StatAction.REDUCE, 500).newValue());
        assertEquals(0, value("hp"));
    }

    @Test
    void barsThatCanExceedTheirMaxAreNotCapped() {
        assertEquals(15, apply("potion", StatAction.SET, 15).newValue());
        assertEquals(50, apply("arcrot", StatAction.ADD, 50).newValue());
        assertEquals(0, apply("potion", StatAction.REDUCE, 99).newValue(), "still floored at 0");
    }

    @Test
    void tempHpAndAcCannotGoNegative() {
        assertEquals(0, apply("temphp", StatAction.SET, -5).newValue());
        assertEquals(0, apply("ac", StatAction.REDUCE, 3).newValue());
    }

    @Test
    void damageHitsTempHpFirst() {
        apply("temphp", StatAction.SET, 10);
        changes.clear();
        StatChange change = apply("hp", StatAction.DAMAGE, 4);
        assertEquals(6, value("temphp"));
        assertEquals(60, value("hp"));
        assertFalse(change.changed());
        assertEquals(List.of(new StatChange(ALICE, stat("temphp"), 10, 6)), changes);
    }

    @Test
    void damageBeyondTempHpComesOffHp() {
        apply("temphp", StatAction.SET, 10);
        changes.clear();
        StatChange change = apply("hp", StatAction.DAMAGE, 25);
        assertEquals(0, value("temphp"));
        assertEquals(45, value("hp"));
        assertEquals(new StatChange(ALICE, stat("hp"), 60, 45), change);
        assertEquals(2, changes.size());
    }

    @Test
    void damageWithoutTempHpIsLikeReduce() {
        apply("hp", StatAction.DAMAGE, 70);
        assertEquals(0, value("hp"));
        assertEquals(0, value("temphp"));
    }

    @Test
    void damageNeedsAShield() {
        assertThrows(IllegalArgumentException.class, () -> apply("potion", StatAction.DAMAGE, 1));
        assertThrows(IllegalArgumentException.class, () -> apply("str", StatAction.DAMAGE, 1));
    }

    // Issue 16: maximums now use stat totals, so a Con modifier raises max HP.
    @Test
    void modifiersAddToTotalsAndMaximums() {
        StoredModifier con = service.addModifier(ALICE, "con", 1);
        assertEquals(1, value("con"));
        assertEquals(70, value("max_hp"));
        // Bug 8: max_potion modifiers did nothing.
        service.addModifier(ALICE, "max_potion", 2);
        assertEquals(12, value("max_potion"));
        assertEquals(List.of(con.id()), repository.modifiers.get(ALICE).stream().map(StoredModifier::id).limit(1).toList());
        assertEquals(2, repository.modifiers.get(ALICE).size());
    }

    @Test
    void modifierTargetsMustExist() {
        assertThrows(IllegalArgumentException.class, () -> service.addModifier(ALICE, "luck", 1));
        assertThrows(IllegalArgumentException.class, () -> service.addModifier(ALICE, "max_str", 1));
        assertTrue(service.isModifierTarget("max_hp"));
        assertTrue(service.isModifierTarget("str"));
    }

    @Test
    void removingAMaxModifierBringsHpDownToTheNewMax() {
        StoredModifier bonus = service.addModifier(ALICE, "max_hp", 10);
        apply("hp", StatAction.ADD, 10);
        assertEquals(70, value("hp"));
        changes.clear();
        assertTrue(service.removeModifier(ALICE, bonus.id()));
        assertEquals(60, value("hp"));
        assertEquals(List.of(new StatChange(ALICE, stat("hp"), 70, 60)), changes);
        assertTrue(repository.modifiers.get(ALICE).isEmpty());
        assertFalse(service.removeModifier(ALICE, bonus.id()));
    }

    @Test
    void loweringConstitutionBringsHpDown() {
        apply("con", StatAction.SET, 2);
        apply("hp", StatAction.ADD, 20);
        assertEquals(80, value("hp"));
        apply("con", StatAction.SET, 0);
        assertEquals(60, value("hp"));
    }

    // Issue 17 (intended): Arcrot modifiers add on top of the item count.
    @Test
    void itemCountsAddToTheCurrentValueWithModifiersOnTop() {
        service.setItemCounts(ALICE, Map.of(
                new ItemTagSource("arcrot", false), 30,
                new ItemTagSource("charged_arcrot", true), 12,
                new ItemTagSource("fictis", false), 2));
        assertEquals(42, value("arcrot"), "can go above the max of 35");
        assertEquals(2, value("fictis"));
        service.addModifier(ALICE, "arcrot", 3);
        assertEquals(45, value("arcrot"));
        // Option (a): a /stats add on arcrot sticks on top of the items.
        apply("arcrot", StatAction.ADD, 1);
        assertEquals(46, value("arcrot"));
        service.setItemCounts(ALICE, Map.of());
        assertEquals(4, value("arcrot"));
        assertEquals(1, repository.values.get(ALICE).get("arcrot"), "item counts are never saved");
    }

    @Test
    void setOnABarSetsItsBase() {
        service.setItemCounts(ALICE, Map.of(new ItemTagSource("fictis", false), 2));
        assertEquals(7, apply("fictis", StatAction.SET, 5).newValue());
    }

    @Test
    void charactersSurviveARestart() {
        apply("str", StatAction.SET, 2);
        apply("hp", StatAction.REDUCE, 15);
        service.addModifier(ALICE, "con", 1);
        StatService restarted = new StatService(defaultStats, repository, change -> { });
        restarted.load();
        assertEquals(2, restarted.value(ALICE, "str").orElseThrow());
        assertEquals(45, restarted.value(ALICE, "hp").orElseThrow());
        assertEquals(70, restarted.value(ALICE, "max_hp").orElseThrow());
    }

    @Test
    void loadingSavesStatsTheCharacterHasNoValueFor() {
        ProfileKey bob = new ProfileKey(UUID.randomUUID(), "main");
        repository.saveValue(bob, "str", 2);
        StatService restarted = new StatService(defaultStats, repository, change -> { });
        restarted.load();
        assertEquals(2, repository.values.get(bob).get("str"), "saved values are kept");
        assertEquals(6, repository.values.get(bob).get("spd"));
        assertEquals(60, repository.values.get(bob).get("hp"));
        assertEquals(defaultStats.all().size(), repository.values.get(bob).size());
    }

    // Bug 6: deleted profiles left their modifiers behind.
    @Test
    void deletingACharacterRemovesEverything() {
        service.addModifier(ALICE, "con", 1);
        service.deleteCharacter(ALICE);
        assertTrue(service.sheet(ALICE).isEmpty());
        assertFalse(repository.values.containsKey(ALICE));
        assertFalse(repository.modifiers.containsKey(ALICE));
    }

    @Test
    void unknownCharactersAreRefused() {
        ProfileKey nobody = new ProfileKey(UUID.randomUUID(), "main");
        assertThrows(IllegalArgumentException.class, () -> service.apply(nobody, stat("str"), StatAction.SET, 1));
        assertThrows(IllegalArgumentException.class, () -> service.addModifier(nobody, "str", 1));
    }

    @Test
    void reloadedDefinitionsApplyAndCapBars() throws InvalidConfigurationException {
        apply("hp", StatAction.REDUCE, 10);
        service.setDefinitions(stats("""
                stats:
                  con: {}
                  hp: {type: bar, max: "40 + {con}", start: max}
                  luck: {baseline: 3}
                """));
        assertEquals(40, value("hp"));
        assertEquals(3, value("luck"));
        assertEquals(3, repository.values.get(ALICE).get("luck"), "new stats are saved at their baseline");
        assertTrue(service.value(ALICE, "str").isEmpty(), "removed stats are hidden");
        assertEquals(0, repository.values.get(ALICE).get("arcrot"), "their saved values are kept");
    }

    @Test
    void aFullBarWhoseMaxReadsItsOwnValueDoesNotLoop() throws InvalidConfigurationException {
        StatService odd = new StatService(stats("stats:\n  a: {type: bar, max: \"{a} + 5\", start: max}\n"),
                new MemoryRepository(), change -> { });
        odd.createCharacter(ALICE);
        assertEquals(5, odd.value(ALICE, "a").orElseThrow());
        assertEquals(10, odd.value(ALICE, "max_a").orElseThrow());
    }
}
