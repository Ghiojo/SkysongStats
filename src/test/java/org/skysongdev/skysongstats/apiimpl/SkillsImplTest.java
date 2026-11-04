package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.SkillChange;
import org.skysongdev.skysongstats.api.SkillLevel;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.UnsavedSkillRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillsImplTest {
    private static final CharacterId ALICE = new CharacterId(UUID.randomUUID(), "Default");
    private static final CharacterId NOBODY = new CharacterId(UUID.randomUUID(), "Default");
    private static final String SKILLS = """
            levels:
              unskilled: {name: "Unskilled", color: "<dark_gray>", xp: 0}
              novice: {name: "Novice", color: "<green>", xp: 16}
              expert: {name: "Expert", color: "<gold>", xp: 128}
            skills:
              mist_gathering:
                name: "Mist Gathering"
                color: "<aqua>"
                aliases: [mist]
              tande:
                name: "Transportation and Exploration"
                short-name: "T&E"
                aliases: [t&e]
              cooking: {name: "Cooking", baseline: 20}
            """;

    private final List<org.skysongdev.skysongstats.character.SkillChange> fired = new ArrayList<>();
    private SkillService service;
    private SkillsImpl skills;

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
        service = new SkillService(config(SKILLS), new UnsavedSkillRepository(line -> { }), fired::add);
        service.createCharacter(new ProfileKey(ALICE.player(), ALICE.profile()));
        skills = new SkillsImpl(service, new MainThread(() -> true), new ChangeCauses());
    }

    private Skill skill(String name) {
        return skills.find(name).orElseThrow();
    }

    @Test
    void allListsEverySkillInFileOrder() {
        assertEquals(List.of("mist_gathering", "tande", "cooking"), skills.all().stream().map(Skill::id).toList());
    }

    @Test
    void aSkillCarriesItsNamesAndColorAsConfigured() {
        assertEquals(new Skill("tande", "Transportation and Exploration", "T&E", "", List.of("t&e")),
                skill("tande"));
        assertEquals(new Skill("mist_gathering", "Mist Gathering", "Mist Gathering", "<aqua>", List.of("mist")),
                skill("mist_gathering"));
    }

    @Test
    void findMatchesIdsAndAliasesIgnoringCase() {
        assertEquals("mist_gathering", skill("MIST_GATHERING").id());
        assertEquals("mist_gathering", skill("Mist").id());
        assertEquals("tande", skill("T&E").id());
    }

    @Test
    void findMatchesNothingElse() {
        assertEquals(Optional.empty(), skills.find("mistgathering"));
        assertEquals(Optional.empty(), skills.find("Mist Gathering"));
        assertEquals(Optional.empty(), skills.find(""));
    }

    @Test
    void levelsAreListedLowestFirst() {
        assertEquals(List.of(new SkillLevel("unskilled", "Unskilled", "<dark_gray>", 0),
                new SkillLevel("novice", "Novice", "<green>", 16),
                new SkillLevel("expert", "Expert", "<gold>", 128)), skills.levels());
    }

    @Test
    void levelForGivesTheHighestLevelReached() {
        assertEquals("unskilled", skills.levelFor(-5).id());
        assertEquals("unskilled", skills.levelFor(15).id());
        assertEquals("novice", skills.levelFor(16).id());
        assertEquals("novice", skills.levelFor(127).id());
        assertEquals("expert", skills.levelFor(128).id());
    }

    @Test
    void xpAndLevelOfACharacter() {
        assertEquals(OptionalInt.of(20), skills.xp(ALICE, skill("cooking")));
        assertEquals(OptionalInt.of(20), skills.xp(ALICE, "cooking"));
        assertEquals(Optional.of("novice"), skills.level(ALICE, "cooking").map(SkillLevel::id));
        assertEquals(Optional.of("unskilled"), skills.level(ALICE, skill("mist")).map(SkillLevel::id));
    }

    @Test
    void readsAreEmptyForACharacterThatDoesNotExist() {
        assertEquals(OptionalInt.empty(), skills.xp(NOBODY, skill("cooking")));
        assertEquals(Optional.empty(), skills.level(NOBODY, skill("cooking")));
    }

    @Test
    void readsAreEmptyForASkillNameThatDoesNotExist() {
        assertEquals(OptionalInt.empty(), skills.xp(ALICE, "smithing"));
        assertEquals(Optional.empty(), skills.level(ALICE, "smithing"));
    }

    @Test
    void aSkillRemovedByAReloadReadsEmpty() {
        Skill mist = skill("mist");
        service.setConfig(config("""
                levels:
                  unskilled: {xp: 0}
                skills:
                  cooking: {}
                """));

        assertEquals(OptionalInt.empty(), skills.xp(ALICE, mist));
        assertEquals(Optional.empty(), skills.setXp(ALICE, mist, 5));
    }

    @Test
    void setXpSavesFiresTheChangeAndReturnsIt() {
        Optional<SkillChange> change = skills.setXp(ALICE, skill("cooking"), 50);

        assertEquals(Optional.of(new SkillChange(ALICE, skill("cooking"), 20, 50)), change);
        assertEquals(OptionalInt.of(50), skills.xp(ALICE, "cooking"));
        assertEquals(1, fired.size());
    }

    @Test
    void addXpAddsAndStopsAtTheLargestInt() {
        assertEquals(30, skills.addXp(ALICE, "cooking", 10).orElseThrow().after());
        assertEquals(Integer.MAX_VALUE, skills.addXp(ALICE, "cooking", Integer.MAX_VALUE).orElseThrow().after());
    }

    @Test
    void aWriteThatChangesNothingFiresNothing() {
        SkillChange change = skills.addXp(ALICE, "cooking", 0).orElseThrow();

        assertFalse(change.changed());
        assertTrue(fired.isEmpty());
    }

    @Test
    void writesAreEmptyForACharacterOrSkillThatDoesNotExist() {
        assertEquals(Optional.empty(), skills.setXp(NOBODY, skill("cooking"), 5));
        assertEquals(Optional.empty(), skills.addXp(NOBODY, "cooking", 5));
        assertEquals(Optional.empty(), skills.addXp(ALICE, "smithing", 5));
        assertTrue(fired.isEmpty());
    }

    @Test
    void negativeAmountsAreRefused() {
        Skill cooking = skill("cooking");

        assertThrows(IllegalArgumentException.class, () -> skills.setXp(ALICE, cooking, -1));
        assertThrows(IllegalArgumentException.class, () -> skills.addXp(ALICE, cooking, -1));
        assertEquals(OptionalInt.of(20), skills.xp(ALICE, cooking));
    }

    @Test
    void characterMethodsRefuseCallsOffTheMainThread() {
        SkillsImpl offThread = new SkillsImpl(service, new MainThread(() -> false), new ChangeCauses());
        Skill cooking = skill("cooking");

        assertThrows(IllegalStateException.class, () -> offThread.xp(ALICE, cooking));
        assertThrows(IllegalStateException.class, () -> offThread.level(ALICE, cooking));
        assertThrows(IllegalStateException.class, () -> offThread.setXp(ALICE, cooking, 1));
        assertThrows(IllegalStateException.class, () -> offThread.addXp(ALICE, cooking, 1));
    }

    @Test
    void skillsYmlLookupsWorkOffTheMainThread() {
        SkillsImpl offThread = new SkillsImpl(service, new MainThread(() -> false), new ChangeCauses());

        assertEquals(3, offThread.all().size());
        assertTrue(offThread.find("mist").isPresent());
        assertEquals(3, offThread.levels().size());
        assertEquals("expert", offThread.levelFor(500).id());
    }

    @Test
    void nullArgumentsAreRefused() {
        Skill cooking = skill("cooking");

        assertThrows(NullPointerException.class, () -> skills.find(null));
        assertThrows(NullPointerException.class, () -> skills.xp(null, cooking));
        assertThrows(NullPointerException.class, () -> skills.xp(ALICE, (Skill) null));
    }
}
