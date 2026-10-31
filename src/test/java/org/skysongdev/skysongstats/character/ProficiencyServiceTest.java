package org.skysongdev.skysongstats.character;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficienciesConfigLoader;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.definition.SkillsConfigLoader;
import org.skysongdev.skysongstats.storage.ProficiencyRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredFocus;
import org.skysongdev.skysongstats.storage.StoredProficiencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link ProficiencyService}, against a small
 * proficiencies.yml (costs 4, 6, 8) and an in-memory repository.
 */
class ProficiencyServiceTest {

    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");
    private static final String SKILLS = """
            levels:
              unskilled: {xp: 0}
            skills:
              martial: {}
              medical: {}
            """;
    private static final String PROFICIENCIES = """
            cost: 4
            tiers:
              untrained: {}
              proficient: {}
              adept: {extra: 2}
              master: {extra: 2}
            proficiencies:
              axes: {skill: martial}
              bows: {skill: martial}
              medicine: {skill: medical}
            """;

    // Keeps saved rows in maps, like the database would.
    private static final class MemoryRepository implements ProficiencyRepository {
        final Map<ProfileKey, Map<String, String>> tiers = new HashMap<>();
        final Map<ProfileKey, StoredFocus> focuses = new HashMap<>();

        @Override
        public void createTables() {
        }

        @Override
        public Map<ProfileKey, StoredProficiencies> loadAll() {
            Map<ProfileKey, StoredProficiencies> all = new HashMap<>();
            tiers.forEach((key, saved) -> all.put(key, new StoredProficiencies(saved, focuses.get(key))));
            return all;
        }

        @Override
        public void saveTier(ProfileKey character, String proficiencyId, String tierId) {
            tiers.computeIfAbsent(character, k -> new HashMap<>()).put(proficiencyId, tierId);
        }

        @Override
        public void saveFocus(ProfileKey character, StoredFocus focus) {
            focuses.put(character, focus);
        }

        @Override
        public void clearFocus(ProfileKey character) {
            focuses.remove(character);
        }

        @Override
        public void deleteCharacter(ProfileKey character) {
            tiers.remove(character);
            focuses.remove(character);
        }
    }

    // Records every change the service reports.
    private static final class RecordingListener implements ProficiencyChangeListener {
        final List<ProficiencyTierChange> tierChanges = new ArrayList<>();
        final List<ProficiencyFocusChange> focusChanges = new ArrayList<>();

        @Override
        public void onTierChange(ProficiencyTierChange change) {
            tierChanges.add(change);
        }

        @Override
        public void onFocusChange(ProficiencyFocusChange change) {
            focusChanges.add(change);
        }
    }

    private MemoryRepository repository;
    private RecordingListener listener;
    private List<String> warnings;
    private ProficiencyService service;

    private static ProficienciesConfig config(String yaml) {
        try {
            YamlConfiguration skills = new YamlConfiguration();
            skills.loadFromString(SKILLS);
            YamlConfiguration file = new YamlConfiguration();
            file.loadFromString(yaml);
            return ProficienciesConfigLoader.load(file, SkillsConfigLoader.load(skills, name -> true).skills(),
                    name -> true);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    @BeforeEach
    void setUp() {
        repository = new MemoryRepository();
        listener = new RecordingListener();
        warnings = new ArrayList<>();
        service = new ProficiencyService(config(PROFICIENCIES), repository, listener, warnings::add);
    }

    private ProficiencyDefinition proficiency(String id) {
        return service.config().proficiencies().get(id).orElseThrow();
    }

    private ProficiencyTier tier(String id) {
        return service.config().tiers().get(id).orElseThrow();
    }

    private String tierOf(String proficiency) {
        return service.tier(ALICE, proficiency(proficiency)).id();
    }

    private ProficiencyFocus focus() {
        return service.focus(ALICE).orElseThrow();
    }

    // Alice focusing axes at the given tier.
    private void focusAxesAt(String tier) {
        service.createCharacter(ALICE);
        service.setTier(ALICE, proficiency("axes"), tier(tier));
        service.changeFocus(ALICE, proficiency("axes"));
        listener.tierChanges.clear();
        listener.focusChanges.clear();
    }

    @Test
    void aNewCharacterSavesEveryProficiencyAtTheFirstTierAndHasNoFocus() {
        service.createCharacter(ALICE);
        assertEquals(Map.of("axes", "untrained", "bows", "untrained", "medicine", "untrained"), repository.tiers.get(ALICE));
        assertEquals("untrained", tierOf("axes"));
        assertTrue(service.focus(ALICE).isEmpty());
        assertEquals(Set.of(ALICE), service.characters());
    }

    @Test
    void anUnknownCharacterHasNoProficiencies() {
        assertFalse(service.hasCharacter(ALICE));
        assertThrows(IllegalArgumentException.class, () -> service.tier(ALICE, proficiency("axes")));
        assertThrows(IllegalArgumentException.class, () -> service.focus(ALICE));
    }

    @Test
    void focusingWithNoPointsSwitchesAtOnce() {
        service.createCharacter(ALICE);
        assertEquals(FocusRequest.FOCUSED, service.requestFocus(ALICE, proficiency("axes")));
        assertEquals(new ProficiencyFocus(proficiency("axes"), 0), focus());
        assertEquals(new StoredFocus("axes", 0), repository.focuses.get(ALICE));
        assertEquals(FocusRequest.FOCUSED, service.requestFocus(ALICE, proficiency("bows")));
        assertEquals("bows", focus().proficiency().id());
        assertEquals(2, listener.focusChanges.size());
    }

    @Test
    void switchingAFocusWithPointsNeedsConfirmation() {
        focusAxesAt("untrained");
        service.train(ALICE, 2, "martial", tier("master"));
        assertEquals(FocusRequest.NEEDS_CONFIRMATION, service.requestFocus(ALICE, proficiency("bows")));
        assertEquals(new ProficiencyFocus(proficiency("axes"), 2), focus());

        service.changeFocus(ALICE, proficiency("bows"));
        assertEquals(new ProficiencyFocus(proficiency("bows"), 0), focus());
    }

    @Test
    void focusingTheCurrentFocusChangesNothing() {
        focusAxesAt("untrained");
        service.train(ALICE, 2, "martial", tier("master"));
        assertEquals(FocusRequest.ALREADY_FOCUSED, service.requestFocus(ALICE, proficiency("axes")));
        assertEquals(2, focus().points());
    }

    @Test
    void aMasteredProficiencyCantBeFocused() {
        service.createCharacter(ALICE);
        service.setTier(ALICE, proficiency("axes"), tier("master"));
        assertEquals(FocusRequest.MASTERED, service.requestFocus(ALICE, proficiency("axes")));
        assertThrows(IllegalArgumentException.class, () -> service.changeFocus(ALICE, proficiency("axes")));
        assertTrue(service.focus(ALICE).isEmpty());
    }

    @Test
    void trainingAddsPointsToAFocusOfTheSameSkill() {
        focusAxesAt("untrained");
        Training training = service.train(ALICE, 3, "martial", tier("master"));
        assertEquals(new Training(Training.Outcome.ADDED, proficiency("axes"), tier("untrained"), 3), training);
        assertTrue(training.added());
        assertEquals(new StoredFocus("axes", 3), repository.focuses.get(ALICE));
        assertEquals(new ProficiencyFocusChange(ALICE, new ProficiencyFocus(proficiency("axes"), 0),
                new ProficiencyFocus(proficiency("axes"), 3)), listener.focusChanges.getFirst());
    }

    @Test
    void reachingTheCostRaisesTheTierAndResetsThePoints() {
        focusAxesAt("untrained");
        service.train(ALICE, 3, "martial", tier("master"));
        Training training = service.train(ALICE, 5, "martial", tier("master"));
        assertEquals(new Training(Training.Outcome.TIER_UP, proficiency("axes"), tier("proficient"), 0), training);
        assertEquals("proficient", tierOf("axes"));
        assertEquals("proficient", repository.tiers.get(ALICE).get("axes"));
        assertEquals(0, focus().points());
        assertEquals(List.of(new ProficiencyTierChange(ALICE, proficiency("axes"), tier("untrained"),
                tier("proficient"), true)), listener.tierChanges);
    }

    @Test
    void costsGrowWithEachTier() {
        focusAxesAt("proficient");
        assertEquals(6, service.pointsNeeded(ALICE, focus()));
        assertEquals(Training.Outcome.ADDED, service.train(ALICE, 5, "martial", tier("master")).outcome());
        assertEquals(Training.Outcome.TIER_UP, service.train(ALICE, 1, "martial", tier("master")).outcome());
        assertEquals(8, service.pointsNeeded(ALICE, focus()));
    }

    @Test
    void oneTrainingGivesAtMostOneTier() {
        focusAxesAt("untrained");
        service.train(ALICE, 100, "martial", tier("master"));
        assertEquals("proficient", tierOf("axes"));
        assertEquals(0, focus().points());
    }

    @Test
    void reachingTheTopTierClearsTheFocus() {
        focusAxesAt("adept");
        Training training = service.train(ALICE, 8, "martial", tier("master"));
        assertEquals(new Training(Training.Outcome.TIER_UP, proficiency("axes"), tier("master"), 0), training);
        assertTrue(service.focus(ALICE).isEmpty());
        assertFalse(repository.focuses.containsKey(ALICE));
        assertNull(listener.focusChanges.getLast().after());
    }

    @Test
    void trainingOfAnotherSkillAddsNothing() {
        focusAxesAt("untrained");
        Training training = service.train(ALICE, 3, "medical", tier("master"));
        assertEquals(new Training(Training.Outcome.OTHER_SKILL, proficiency("axes"), tier("untrained"), 0), training);
        assertFalse(training.added());
        assertEquals(0, focus().points());
        assertTrue(listener.focusChanges.isEmpty());
    }

    @Test
    void aTrainerCantTrainAtOrPastTheirOwnTier() {
        focusAxesAt("proficient");
        assertEquals(Training.Outcome.AT_MAX_TIER, service.train(ALICE, 3, "martial", tier("proficient")).outcome());
        assertEquals(Training.Outcome.AT_MAX_TIER, service.train(ALICE, 3, "martial", tier("untrained")).outcome());
        assertEquals(0, focus().points());
        assertEquals(Training.Outcome.ADDED, service.train(ALICE, 3, "martial", tier("adept")).outcome());
    }

    @Test
    void aTrainerCanTrainUpToTheirOwnTier() {
        focusAxesAt("untrained");
        assertEquals(Training.Outcome.TIER_UP, service.train(ALICE, 4, "martial", tier("proficient")).outcome());
        assertEquals(Training.Outcome.AT_MAX_TIER, service.train(ALICE, 4, "martial", tier("proficient")).outcome());
    }

    @Test
    void trainingWithoutAFocusAddsNothing() {
        service.createCharacter(ALICE);
        assertEquals(new Training(Training.Outcome.NO_FOCUS, null, null, 0),
                service.train(ALICE, 3, "martial", tier("master")));
    }

    @Test
    void trainingNeedsAPositiveAmount() {
        focusAxesAt("untrained");
        assertThrows(IllegalArgumentException.class, () -> service.train(ALICE, 0, "martial", tier("master")));
        assertThrows(IllegalArgumentException.class, () -> service.train(ALICE, -1, "martial", tier("master")));
    }

    @Test
    void removingPointsStopsAtZeroAndNeverLowersATier() {
        focusAxesAt("proficient");
        service.train(ALICE, 3, "martial", tier("master"));
        assertEquals(1, service.removePoints(ALICE, 2).orElseThrow().points());
        assertEquals(0, service.removePoints(ALICE, 10).orElseThrow().points());
        assertEquals("proficient", tierOf("axes"));
        assertThrows(IllegalArgumentException.class, () -> service.removePoints(ALICE, -1));
    }

    @Test
    void settingPointsMustStayBelowTheNextTiersCost() {
        focusAxesAt("untrained");
        assertEquals(3, service.setPoints(ALICE, 3).orElseThrow().points());
        assertEquals(new StoredFocus("axes", 3), repository.focuses.get(ALICE));
        assertThrows(IllegalArgumentException.class, () -> service.setPoints(ALICE, 4));
        assertThrows(IllegalArgumentException.class, () -> service.setPoints(ALICE, -1));
        assertEquals(3, focus().points());
    }

    @Test
    void pointsCommandsWithoutAFocusDoNothing() {
        service.createCharacter(ALICE);
        assertTrue(service.removePoints(ALICE, 1).isEmpty());
        assertTrue(service.setPoints(ALICE, 1).isEmpty());
        assertTrue(listener.focusChanges.isEmpty());
    }

    @Test
    void setTierRaisesOrLowersAndReportsIt() {
        service.createCharacter(ALICE);
        ProficiencyTierChange up = service.setTier(ALICE, proficiency("bows"), tier("adept"));
        assertTrue(up.tierUp());
        ProficiencyTierChange down = service.setTier(ALICE, proficiency("bows"), tier("proficient"));
        assertFalse(down.tierUp());
        assertEquals(List.of(up, down), listener.tierChanges);
        assertEquals("proficient", repository.tiers.get(ALICE).get("bows"));
    }

    @Test
    void settingTheSameTierIsNotReported() {
        service.createCharacter(ALICE);
        service.setTier(ALICE, proficiency("bows"), tier("untrained"));
        assertTrue(listener.tierChanges.isEmpty());
    }

    @Test
    void setTierKeepsTheFocusUnlessItBecomesTheTopTier() {
        focusAxesAt("untrained");
        service.train(ALICE, 2, "martial", tier("master"));
        service.setTier(ALICE, proficiency("axes"), tier("adept"));
        assertEquals(new ProficiencyFocus(proficiency("axes"), 2), focus());
        service.setTier(ALICE, proficiency("axes"), tier("master"));
        assertTrue(service.focus(ALICE).isEmpty());
    }

    @Test
    void loadingRestoresTiersAndFocusAndFillsInMissingProficiencies() {
        repository.saveTier(ALICE, "axes", "adept");
        repository.saveFocus(ALICE, new StoredFocus("axes", 5));
        service.load();
        assertEquals("adept", tierOf("axes"));
        assertEquals(new ProficiencyFocus(proficiency("axes"), 5), focus());
        assertEquals("untrained", repository.tiers.get(ALICE).get("bows"));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void aSavedTierThatLeftTheFileCountsAsTheFirstWithAWarning() {
        repository.saveTier(ALICE, "axes", "grandmaster");
        service.load();
        assertEquals("untrained", tierOf("axes"));
        assertEquals(List.of("Saved proficiency tiers [grandmaster] are not in proficiencies.yml, they count as untrained"),
                warnings);
    }

    @Test
    void aReloadSavesNewProficienciesAndAFocusOnARemovedOneShowsAsNone() {
        focusAxesAt("proficient");
        service.setConfig(config(PROFICIENCIES.replace("axes: {skill: martial}", "spears: {skill: martial}")));
        assertEquals("untrained", repository.tiers.get(ALICE).get("spears"));
        assertEquals("proficient", repository.tiers.get(ALICE).get("axes"));
        assertTrue(service.focus(ALICE).isEmpty());
        assertEquals(Training.Outcome.NO_FOCUS, service.train(ALICE, 1, "martial", tier("master")).outcome());

        service.setConfig(config(PROFICIENCIES));
        assertEquals("axes", focus().proficiency().id());
        assertEquals("proficient", tierOf("axes"));
    }

    @Test
    void ensureCharacterOnlyCreatesMissingCharacters() {
        service.createCharacter(ALICE);
        service.setTier(ALICE, proficiency("axes"), tier("adept"));
        service.ensureCharacter(ALICE);
        assertEquals("adept", tierOf("axes"));
        ProfileKey alt = new ProfileKey(ALICE.player(), "alt");
        service.ensureCharacter(alt);
        assertEquals("untrained", service.tier(alt, proficiency("axes")).id());
    }

    @Test
    void deletingACharacterRemovesItsTiersAndFocus() {
        focusAxesAt("proficient");
        service.deleteCharacter(ALICE);
        assertFalse(service.hasCharacter(ALICE));
        assertFalse(repository.tiers.containsKey(ALICE));
        assertFalse(repository.focuses.containsKey(ALICE));
    }
}
