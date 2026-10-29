package org.skysongdev.skysongstats.service.downtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DowntimeServiceTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private InMemoryDowntimeRepository repository;
    private List<DowntimeChange> events;
    private DowntimeService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDowntimeRepository();
        events = new ArrayList<>();
        service = new DowntimeService(repository, DowntimeSettings.defaults(), events::add);
    }

    private void reload() {
        service = new DowntimeService(repository, DowntimeSettings.defaults(), events::add);
        service.load();
    }

    // Issue 31: players and profiles from before downtime have no rows.

    @Test
    void missingGlobalIsCreatedAtStartAndSaved() {
        assertEquals(3, service.getGlobal(ALICE));
        assertEquals(3, repository.globals.get(ALICE));
    }

    @Test
    void missingCharacterIsCreatedAtMaxAndSaved() {
        assertEquals(3, service.getCharacter(ALICE, "Default"));
        assertEquals(3, repository.characters.get(new CharacterKey(ALICE, "Default")));
    }

    @Test
    void readingAnExistingValueDoesNotWrite() {
        repository.globals.put(ALICE, 5);
        service.load();
        assertEquals(5, service.getGlobal(ALICE));
        assertEquals(0, repository.writes);
    }

    @Test
    void creatingARowFiresNoEvent() {
        service.getGlobal(ALICE);
        service.getCharacter(ALICE, "Default");
        assertTrue(events.isEmpty());
    }

    // Issue 32: add to a profile used the global pool.

    @Test
    void addToProfileChangesOnlyThatProfile() {
        repository.globals.put(ALICE, 7);
        repository.characters.put(new CharacterKey(ALICE, "Mage"), 0);
        repository.characters.put(new CharacterKey(ALICE, "Rogue"), 1);
        service.load();

        DowntimeChange change = service.addCharacter(ALICE, "Mage", 2);

        assertEquals(new DowntimeChange(ALICE, DowntimePool.CHARACTER, "Mage", 0, 2, DowntimeChange.Cause.ADD), change);
        assertEquals(2, service.getCharacter(ALICE, "Mage"));
        assertEquals(1, service.getCharacter(ALICE, "Rogue"));
        assertEquals(7, service.getGlobal(ALICE));
    }

    // Answer 2: add caps at the pool's maximum.

    @Test
    void addGlobalStopsAtGlobalMax() {
        repository.globals.put(ALICE, 8);
        service.load();
        DowntimeChange change = service.addGlobal(ALICE, 5);
        assertEquals(9, change.newValue());
        assertEquals(1, change.delta());
    }

    @Test
    void addCharacterStopsAtCharacterMax() {
        repository.characters.put(new CharacterKey(ALICE, "Default"), 1);
        service.load();
        assertEquals(3, service.addCharacter(ALICE, "Default", 10).newValue());
    }

    @Test
    void addNeverLowersAValueAlreadyOverTheMax() {
        repository.globals.put(ALICE, 12);
        service.load();
        DowntimeChange change = service.addGlobal(ALICE, 1);
        assertEquals(12, change.newValue());
        assertTrue(events.isEmpty());
    }

    // Answer 1: remove clamps at 0.

    @Test
    void removeGlobalStopsAtZero() {
        repository.globals.put(ALICE, 2);
        service.load();
        DowntimeChange change = service.removeGlobal(ALICE, 5);
        assertEquals(0, change.newValue());
        assertEquals(-2, change.delta());
        assertEquals(0, repository.globals.get(ALICE));
    }

    @Test
    void removeCharacterStopsAtZero() {
        assertEquals(0, service.removeCharacter(ALICE, "Default", 4).newValue());
    }

    @Test
    void negativeAmountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addGlobal(ALICE, -1));
        assertThrows(IllegalArgumentException.class, () -> service.removeCharacter(ALICE, "Default", -1));
    }

    @Test
    void changesSurviveAReload() {
        service.addGlobal(ALICE, 2);
        service.removeCharacter(ALICE, "Default", 1);
        reload();
        assertEquals(5, service.getGlobal(ALICE));
        assertEquals(2, service.getCharacter(ALICE, "Default"));
    }

    @Test
    void eachChangeFiresOneEvent() {
        service.addGlobal(ALICE, 1);
        service.removeCharacter(ALICE, "Default", 1);
        assertEquals(List.of(
                new DowntimeChange(ALICE, DowntimePool.GLOBAL, null, 3, 4, DowntimeChange.Cause.ADD),
                new DowntimeChange(ALICE, DowntimePool.CHARACTER, "Default", 3, 2, DowntimeChange.Cause.REMOVE)
        ), events);
    }

    @Test
    void failedSaveLeavesValueAndFiresNoEvent() {
        service.getGlobal(ALICE);
        repository.failWrites = true;
        assertThrows(IllegalStateException.class, () -> service.addGlobal(ALICE, 1));
        assertEquals(3, service.getGlobal(ALICE));
        assertTrue(events.isEmpty());
    }

    // Issue 34: the preference was not saved, and a missing one meant different things.

    @Test
    void missingPreferenceMeansCharacter() {
        assertFalse(service.spendsGlobal(ALICE));
    }

    @Test
    void firstToggleSwitchesToGlobalAndIsSaved() {
        assertTrue(service.togglePreference(ALICE));
        assertTrue(repository.spendsGlobal.get(ALICE));
        reload();
        assertTrue(service.spendsGlobal(ALICE));
        assertFalse(service.togglePreference(ALICE));
    }

    @Test
    void removeFromPreferredUsesActiveProfileByDefault() {
        DowntimeChange change = service.removeFromPreferred(ALICE, "Mage", 1);
        assertEquals(DowntimePool.CHARACTER, change.pool());
        assertEquals("Mage", change.profile());
        assertEquals(3, service.getGlobal(ALICE));
    }

    @Test
    void removeFromPreferredUsesGlobalWhenPreferred() {
        service.setPreference(ALICE, true);
        DowntimeChange change = service.removeFromPreferred(ALICE, "Mage", 1);
        assertEquals(DowntimePool.GLOBAL, change.pool());
        assertEquals(3, service.getCharacter(ALICE, "Mage"));
    }

    // Issue 37: deleted profiles kept their row.

    @Test
    void deleteCharacterRemovesTheRow() {
        service.getCharacter(ALICE, "Mage");
        service.deleteCharacter(ALICE, "Mage");
        assertFalse(repository.characters.containsKey(new CharacterKey(ALICE, "Mage")));
    }

    // Issue 33: resetall was not saved; and missing rows must still be refilled.

    @Test
    void resetAllRefillsAndSaves() {
        repository.globals.put(ALICE, 7);
        repository.globals.put(BOB, 0);
        repository.characters.put(new CharacterKey(ALICE, "Mage"), 0);
        repository.characters.put(new CharacterKey(BOB, "Default"), 3);
        service.load();

        service.resetAll(Map.of());
        reload();

        assertEquals(9, service.getGlobal(ALICE));
        assertEquals(3, service.getGlobal(BOB));
        assertEquals(3, service.getCharacter(ALICE, "Mage"));
        assertEquals(3, service.getCharacter(BOB, "Default"));
    }

    @Test
    void resetAllCreatesMissingRowsForKnownProfiles() {
        service.resetAll(Map.of(ALICE, List.of("Default", "Mage")));

        assertEquals(6, repository.globals.get(ALICE));
        assertEquals(3, repository.characters.get(new CharacterKey(ALICE, "Default")));
        assertEquals(3, repository.characters.get(new CharacterKey(ALICE, "Mage")));
    }

    @Test
    void resetAllReportsOnlyValuesThatChanged() {
        repository.globals.put(ALICE, 9);
        repository.characters.put(new CharacterKey(ALICE, "Mage"), 1);
        repository.characters.put(new CharacterKey(ALICE, "Rogue"), 3);
        service.load();

        List<DowntimeChange> changes = service.resetAll(Map.of());

        assertEquals(List.of(new DowntimeChange(ALICE, DowntimePool.CHARACTER, "Mage", 1, 3, DowntimeChange.Cause.RESET)), changes);
        assertEquals(changes, events);
    }

    @Test
    void resetAllKeepsAGlobalAlreadyOverTheMax() {
        repository.globals.put(ALICE, 11);
        service.load();
        service.resetAll(Map.of());
        assertEquals(11, service.getGlobal(ALICE));
    }

    @Test
    void customSettingsAreUsed() {
        service = new DowntimeService(repository, new DowntimeSettings(5, 1, 2, 4), events::add);
        assertEquals(1, service.getGlobal(ALICE));
        assertEquals(5, service.getCharacter(ALICE, "Default"));
        assertEquals(4, service.addGlobal(ALICE, 10).newValue());
        service.resetAll(Map.of(BOB, List.of()));
        assertEquals(3, service.getGlobal(BOB));
    }

    @Test
    void newSettingsApplyToTheNextChange() {
        service.addGlobal(ALICE, 0);
        service.setSettings(new DowntimeSettings(5, 1, 4, 12));

        assertEquals(new DowntimeSettings(5, 1, 4, 12), service.getSettings());
        assertEquals(3, service.getGlobal(ALICE));
        assertEquals(12, service.addGlobal(ALICE, 20).newValue());
        assertEquals(5, service.getCharacter(ALICE, "Default"));
        assertEquals(1, service.getGlobal(BOB));
        service.resetAll(Map.of());
        assertEquals(5, service.getGlobal(BOB));
    }

    @Test
    void globalChangesHaveNoProfile() {
        assertNull(service.addGlobal(ALICE, 1).profile());
    }
}
