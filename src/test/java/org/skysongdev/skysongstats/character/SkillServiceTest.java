package org.skysongdev.skysongstats.character;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.SkillRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link SkillService}, against a small skills.yml and an
 * in-memory repository.
 */
class SkillServiceTest {

    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");
    private static final String SKILLS = """
            levels:
              unskilled: {xp: 0}
              novice: {xp: 16}
              expert: {xp: 128}
            skills:
              farming: {}
              cooking: {baseline: 5}
            """;

    // Keeps saved XP in maps, like the database would.
    private static final class MemoryRepository implements SkillRepository {
        final Map<ProfileKey, Map<String, Integer>> xp = new HashMap<>();
        int saves;

        @Override
        public void createTables() {
        }

        @Override
        public Map<ProfileKey, Map<String, Integer>> loadAll() {
            return xp;
        }

        @Override
        public void saveXp(ProfileKey character, String skillId, int value) {
            saves++;
            xp.computeIfAbsent(character, k -> new HashMap<>()).put(skillId, value);
        }

        @Override
        public void deleteCharacter(ProfileKey character) {
            xp.remove(character);
        }
    }

    private MemoryRepository repository;
    private List<SkillChange> changes;
    private SkillService service;

    private static SkillsConfig config(String yaml) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return SkillsConfigLoader.load(file, name -> true);
    }

    @BeforeEach
    void setUp() {
        repository = new MemoryRepository();
        changes = new ArrayList<>();
        service = new SkillService(config(SKILLS), repository, changes::add);
    }

    private SkillDefinition skill(String id) {
        return service.config().skills().get(id).orElseThrow();
    }

    private int xp(String skill) {
        return service.xp(ALICE, skill(skill)).orElseThrow();
    }

    @Test
    void aNewCharacterSavesEverySkillAtItsBaseline() {
        service.createCharacter(ALICE);
        assertEquals(Map.of("farming", 0, "cooking", 5), repository.xp.get(ALICE));
        assertEquals(5, xp("cooking"));
        assertTrue(service.hasCharacter(ALICE));
        assertEquals(Set.of(ALICE), service.characters());
    }

    @Test
    void anUnknownCharacterHasNoXp() {
        assertTrue(service.xp(ALICE, skill("farming")).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.setXp(ALICE, skill("farming"), 1));
        assertThrows(IllegalArgumentException.class, () -> service.level(ALICE, skill("farming")));
    }

    @Test
    void settingXpSavesItAndReportsTheChange() {
        service.createCharacter(ALICE);
        SkillChange change = service.setXp(ALICE, skill("farming"), 16);
        assertEquals(new SkillChange(ALICE, skill("farming"), 0, 16), change);
        assertEquals(List.of(change), changes);
        assertEquals(16, repository.xp.get(ALICE).get("farming"));
    }

    @Test
    void settingTheSameXpIsNotReported() {
        service.createCharacter(ALICE);
        assertFalse(service.setXp(ALICE, skill("farming"), 0).changed());
        assertTrue(changes.isEmpty());
    }

    @Test
    void settingOneSkillLeavesTheOthersAlone() {
        service.createCharacter(ALICE);
        service.setXp(ALICE, skill("farming"), 40);
        assertEquals(5, xp("cooking"));
    }

    @Test
    void addingXpAddsToTheCurrentXp() {
        service.createCharacter(ALICE);
        service.addXp(ALICE, skill("cooking"), 11);
        assertEquals(16, xp("cooking"));
        assertEquals("novice", service.level(ALICE, skill("cooking")).id());
    }

    @Test
    void addingXpStopsAtTheLargestWholeNumber() {
        service.createCharacter(ALICE);
        service.setXp(ALICE, skill("farming"), Integer.MAX_VALUE - 1);
        service.addXp(ALICE, skill("farming"), 10);
        assertEquals(Integer.MAX_VALUE, xp("farming"));
    }

    @Test
    void xpCantBeNegative() {
        service.createCharacter(ALICE);
        assertThrows(IllegalArgumentException.class, () -> service.setXp(ALICE, skill("farming"), -1));
        assertThrows(IllegalArgumentException.class, () -> service.addXp(ALICE, skill("farming"), -1));
        assertEquals(0, xp("farming"));
    }

    @Test
    void theLevelFollowsTheXp() {
        service.createCharacter(ALICE);
        assertEquals("unskilled", service.level(ALICE, skill("farming")).id());
        service.setXp(ALICE, skill("farming"), 128);
        assertEquals("expert", service.level(ALICE, skill("farming")).id());
    }

    @Test
    void loadingFillsInSkillsTheCharacterHasNoXpFor() {
        repository.xp.put(ALICE, new HashMap<>(Map.of("farming", 20)));
        service.load();
        assertEquals(20, xp("farming"));
        assertEquals(5, repository.xp.get(ALICE).get("cooking"));
    }

    @Test
    void ensureCharacterOnlyCreatesMissingCharacters() {
        service.createCharacter(ALICE);
        service.setXp(ALICE, skill("farming"), 30);
        service.ensureCharacter(ALICE);
        assertEquals(30, xp("farming"));
        ProfileKey alt = new ProfileKey(ALICE.player(), "alt");
        service.ensureCharacter(alt);
        assertEquals(5, service.xp(alt, skill("cooking")).orElseThrow());
    }

    @Test
    void deletingACharacterRemovesItsXp() {
        service.createCharacter(ALICE);
        service.deleteCharacter(ALICE);
        assertFalse(service.hasCharacter(ALICE));
        assertFalse(repository.xp.containsKey(ALICE));
    }

    @Test
    void aReloadSavesNewSkillsAndKeepsTheXpOfRemovedOnes() {
        service.createCharacter(ALICE);
        service.setXp(ALICE, skill("farming"), 40);
        service.setConfig(config(SKILLS.replace("farming: {}", "mining: {baseline: 2}")));
        assertEquals(2, xp("mining"));
        assertEquals(40, repository.xp.get(ALICE).get("farming"));
        service.setConfig(config(SKILLS));
        assertEquals(40, xp("farming"));
    }

    @Test
    void aReloadDoesNotSaveSkillsAgain() {
        service.createCharacter(ALICE);
        int saves = repository.saves;
        service.setConfig(config(SKILLS));
        assertEquals(saves, repository.saves);
    }
}
