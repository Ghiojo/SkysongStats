package org.skysongdev.skysongstats.character;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.AncestriesConfigLoader;
import org.skysongdev.skysongstats.storage.AncestryRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAncestry;
import org.skysongdev.skysongstats.storage.StoredAncestryAdjustment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.skysongdev.skysongstats.character.AncestryFixtures.ANCESTRIES;
import static org.skysongdev.skysongstats.character.AncestryFixtures.STATS;
import static org.skysongdev.skysongstats.character.AncestryFixtures.ancestry;
import static org.skysongdev.skysongstats.character.AncestryFixtures.yaml;

/**
 * Plain JUnit tests for {@link AncestryService}, with base values and saved
 * ancestries kept in maps.
 */
class AncestryServiceTest {
    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "Default");
    private static final ProfileKey BOB = new ProfileKey(UUID.randomUUID(), "Default");
    private static final AncestryPicks HUMAN_PICKS = AncestryPicks.none()
            .withStats(0, List.of("str", "con")).withStats(1, List.of("str"));

    private final Map<ProfileKey, StoredAncestry> saved = new LinkedHashMap<>();
    private final Map<String, Integer> base = new HashMap<>();
    private final List<AncestryService.AncestryChange> changes = new ArrayList<>();
    private AncestryService service;

    private final class MemoryRepository implements AncestryRepository {
        @Override
        public void createTables() {
            // Nothing to create in memory.
        }

        @Override
        public Map<ProfileKey, StoredAncestry> loadAll() {
            return Map.copyOf(saved);
        }

        @Override
        public void save(ProfileKey character, StoredAncestry ancestry) {
            saved.put(character, ancestry);
        }

        @Override
        public void delete(ProfileKey character) {
            saved.remove(character);
        }
    }

    @BeforeEach
    void setUp() {
        service = new AncestryService(ANCESTRIES, new MemoryRepository(),
                (character, stat, delta) -> base.merge(stat, delta, Integer::sum), changes::add);
        STATS.all().forEach(stat -> base.put(stat.id(), stat.baseline()));
    }

    private Map<String, Integer> base() {
        return Map.copyOf(base);
    }

    private static StoredAncestryAdjustment adjustment(String stat, int amount, String source) {
        return new StoredAncestryAdjustment(stat, amount, source);
    }

    @Test
    void settingAnAncestryWithoutChoicesPutsItsStatsOn() {
        service.set(ALICE, ancestry("dwarf"), Optional.empty());

        assertEquals(Map.of("str", 0, "dex", 0, "con", 1, "spd", 5), base());
        StoredAncestry dwarf = new StoredAncestry("dwarf", false,
                List.of(adjustment("con", 1, "fixed"), adjustment("spd", -1, "fixed")));
        assertEquals(Optional.of(dwarf), service.of(ALICE));
        assertEquals(Map.of(ALICE, dwarf), saved);
        assertEquals(List.of(new AncestryService.AncestryChange(ALICE, Optional.empty(), Optional.of(dwarf))), changes);
    }

    @Test
    void settingAnAncestryWithPicksPutsEverythingOn() {
        service.set(ALICE, ancestry("human"), Optional.of(HUMAN_PICKS));

        // dex +1 fixed, str +1 and con +1 boosts, str -1 flaw.
        assertEquals(Map.of("str", 0, "dex", 1, "con", 1, "spd", 6), base());
        assertFalse(service.of(ALICE).orElseThrow().choicesPending());
        assertEquals(4, service.of(ALICE).orElseThrow().adjustments().size());
    }

    @Test
    void withoutPicksTheChoicesArePendingUntilChosen() {
        service.set(ALICE, ancestry("human"), Optional.empty());
        assertTrue(service.of(ALICE).orElseThrow().choicesPending());
        assertEquals(1, base.get("dex"));
        assertEquals(0, base.get("str"));

        assertTrue(service.choose(ALICE, HUMAN_PICKS).isPresent());

        assertFalse(service.of(ALICE).orElseThrow().choicesPending());
        assertEquals(Map.of("str", 0, "dex", 1, "con", 1, "spd", 6), base());
        assertEquals(2, changes.size());
    }

    @Test
    void choosingNeedsPendingChoices() {
        assertTrue(service.choose(ALICE, HUMAN_PICKS).isEmpty());
        service.set(ALICE, ancestry("human"), Optional.of(HUMAN_PICKS));
        assertTrue(service.choose(ALICE, HUMAN_PICKS).isEmpty());
    }

    @Test
    void invalidPicksChangeNothing() {
        AncestryPicks bad = AncestryPicks.none().withStats(0, List.of("dex", "str")).withStats(1, List.of("con"));
        assertThrows(IllegalArgumentException.class, () -> service.set(ALICE, ancestry("human"), Optional.of(bad)));
        assertTrue(service.of(ALICE).isEmpty());

        service.set(ALICE, ancestry("human"), Optional.empty());
        Map<String, Integer> before = base();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.choose(ALICE, bad));
        assertTrue(error.getMessage().contains("can't pick 'dex'"), error.getMessage());
        assertEquals(before, base());
        assertTrue(service.of(ALICE).orElseThrow().choicesPending());
    }

    @Test
    void changingAncestryTakesTheOldOneOffExactly() {
        service.set(ALICE, ancestry("human"), Optional.of(HUMAN_PICKS));
        base.merge("str", 3, Integer::sum); // a /stats add since then stays

        service.set(ALICE, ancestry("dwarf"), Optional.empty());

        assertEquals(Map.of("str", 3, "dex", 0, "con", 1, "spd", 5), base());
        assertEquals("dwarf", service.of(ALICE).orElseThrow().ancestry());
        assertEquals("human", changes.getLast().before().orElseThrow().ancestry());
    }

    @Test
    void settingTheSameAncestryAgainPutsItsCurrentVersionOn() {
        service.set(ALICE, ancestry("dwarf"), Optional.empty());
        service.setDefinitions(AncestriesConfigLoader.load(yaml(
                "ancestries:\n  dwarf: {icon: STONE, slot: 10, stats: {con: 2}}\n"), STATS, name -> true));

        // A reload changes nothing by itself...
        assertEquals(1, base.get("con"));
        // ...setting it again does.
        service.set(ALICE, service.definitions().get("dwarf").orElseThrow(), Optional.empty());
        assertEquals(Map.of("str", 0, "dex", 0, "con", 2, "spd", 6), base());
    }

    @Test
    void rechooseTakesOffOnlyTheChoices() {
        service.set(ALICE, ancestry("human"), Optional.of(HUMAN_PICKS));

        assertTrue(service.rechoose(ALICE).isPresent());

        assertEquals(Map.of("str", 0, "dex", 1, "con", 0, "spd", 6), base());
        StoredAncestry human = service.of(ALICE).orElseThrow();
        assertTrue(human.choicesPending());
        assertEquals(List.of(adjustment("dex", 1, "fixed")), human.adjustments());

        AncestryPicks other = AncestryPicks.none().withStats(0, List.of("str", "spd")).withStats(1, List.of("con"));
        service.choose(ALICE, other);
        assertEquals(Map.of("str", 1, "dex", 1, "con", -1, "spd", 7), base());
    }

    @Test
    void rechooseNeedsAnAncestryWithChoices() {
        assertTrue(service.rechoose(ALICE).isEmpty());
        service.set(ALICE, ancestry("dwarf"), Optional.empty());
        assertTrue(service.rechoose(ALICE).isEmpty());
    }

    @Test
    void anAncestryThatLeftTheFileCantBeChosenOrRechosen() {
        service.set(ALICE, ancestry("human"), Optional.empty());
        service.setDefinitions(AncestriesConfigLoader.load(yaml("ancestries: {}\n"), STATS, name -> true));

        assertTrue(service.definitionOf(ALICE).isEmpty());
        assertTrue(service.choose(ALICE, HUMAN_PICKS).isEmpty());
        assertTrue(service.rechoose(ALICE).isEmpty());
        // It is still the character's ancestry, and can still be removed.
        assertEquals("human", service.of(ALICE).orElseThrow().ancestry());
        assertTrue(service.remove(ALICE).isPresent());
        assertEquals(0, base.get("dex"));
    }

    @Test
    void removeTakesEverythingOff() {
        service.set(ALICE, ancestry("human"), Optional.of(HUMAN_PICKS));

        AncestryService.AncestryChange change = service.remove(ALICE).orElseThrow();

        assertEquals(Map.of("str", 0, "dex", 0, "con", 0, "spd", 6), base());
        assertTrue(service.of(ALICE).isEmpty());
        assertTrue(saved.isEmpty());
        assertTrue(change.after().isEmpty());
        assertTrue(service.remove(ALICE).isEmpty());
    }

    @Test
    void deletingACharacterForgetsItWithoutTouchingStats() {
        service.set(ALICE, ancestry("dwarf"), Optional.empty());
        service.set(BOB, ancestry("dwarf"), Optional.empty());
        Map<String, Integer> before = base();
        int changesBefore = changes.size();

        service.deleteCharacter(ALICE);
        service.deleteCharacter(ALICE);

        assertEquals(before, base());
        assertEquals(changesBefore, changes.size());
        assertEquals(Set.of(BOB), service.characters());
        assertEquals(Set.of(BOB), saved.keySet());
    }

    @Test
    void loadReadsWhatWasSaved() {
        StoredAncestry dwarf = new StoredAncestry("dwarf", false, List.of(adjustment("con", 1, "fixed")));
        saved.put(ALICE, dwarf);

        service.load();

        assertEquals(Optional.of(dwarf), service.of(ALICE));
        assertEquals(Optional.of(ancestry("dwarf")), service.definitionOf(ALICE));
        assertEquals(Map.of("str", 0, "dex", 0, "con", 0, "spd", 6), base(), "loading never moves stats");
    }
}
