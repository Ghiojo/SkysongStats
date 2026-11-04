package org.skysongdev.skysongstats.apiimpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilesImplTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final Set<ProfileKey> characters = new HashSet<>();
    private ProfilesImpl profiles;

    @BeforeEach
    void setUp() {
        characters.add(new ProfileKey(ALICE, "Default"));
        characters.add(new ProfileKey(ALICE, "mage"));
        characters.add(new ProfileKey(ALICE, "Bard"));
        characters.add(new ProfileKey(BOB, "Default"));
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(ALICE.toString(), "Bard")));
        ProfileManager.setupProfiles = new ArrayList<>(List.of(
                new SetupProfile(ALICE.toString(), "Default", true),
                new SetupProfile(ALICE.toString(), "Bard", false)));
        profiles = new ProfilesImpl(new ProfileManager(), () -> Set.copyOf(characters), new MainThread(() -> true));
    }

    @AfterEach
    void tearDown() {
        ProfileManager.activeProfiles = null;
        ProfileManager.setupProfiles = null;
    }

    @Test
    void activeIsTheProfileThePlayerIsPlaying() {
        assertEquals(Optional.of(new CharacterId(ALICE, "Bard")), profiles.active(ALICE));
    }

    @Test
    void activeIsEmptyForAPlayerWithNoActiveProfile() {
        assertEquals(Optional.empty(), profiles.active(BOB));
    }

    @Test
    void activeIsEmptyWhenTheProfilesCouldNotBeLoaded() {
        ProfileManager.activeProfiles = null;

        assertEquals(Optional.empty(), profiles.active(ALICE));
    }

    @Test
    void allListsOnlyThatPlayersProfilesSortedIgnoringCase() {
        assertEquals(List.of(new CharacterId(ALICE, "Bard"), new CharacterId(ALICE, "Default"),
                new CharacterId(ALICE, "mage")), profiles.all(ALICE));
    }

    @Test
    void allIsEmptyForAPlayerWithNoProfiles() {
        assertEquals(List.of(), profiles.all(UUID.randomUUID()));
    }

    @Test
    void allCannotBeChanged() {
        List<CharacterId> all = profiles.all(ALICE);

        assertThrows(UnsupportedOperationException.class, () -> all.add(new CharacterId(BOB, "Default")));
    }

    @Test
    void existsMatchesTheProfileNameExactly() {
        assertTrue(profiles.exists(new CharacterId(ALICE, "mage")));
        assertFalse(profiles.exists(new CharacterId(ALICE, "Mage")));
        assertFalse(profiles.exists(new CharacterId(BOB, "Bard")));
    }

    @Test
    void isSetUpFollowsTheSetupRow() {
        assertTrue(profiles.isSetUp(new CharacterId(ALICE, "Default")));
        assertFalse(profiles.isSetUp(new CharacterId(ALICE, "Bard")));
    }

    @Test
    void isSetUpIsFalseForAProfileWithoutASetupRow() {
        assertFalse(profiles.isSetUp(new CharacterId(ALICE, "mage")));
        assertFalse(profiles.isSetUp(new CharacterId(BOB, "Nobody")));
    }

    @Test
    void isSetUpIsFalseWhenTheSetupRowsCouldNotBeLoaded() {
        ProfileManager.setupProfiles = null;

        assertFalse(profiles.isSetUp(new CharacterId(ALICE, "Default")));
    }

    @Test
    void everyMethodRefusesCallsOffTheMainThread() {
        ProfilesImpl offThread = new ProfilesImpl(new ProfileManager(), () -> Set.copyOf(characters),
                new MainThread(() -> false));
        CharacterId alice = new CharacterId(ALICE, "Default");

        assertThrows(IllegalStateException.class, () -> offThread.active(ALICE));
        assertThrows(IllegalStateException.class, () -> offThread.all(ALICE));
        assertThrows(IllegalStateException.class, () -> offThread.exists(alice));
        assertThrows(IllegalStateException.class, () -> offThread.isSetUp(alice));
    }

    @Test
    void nullArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> profiles.active(null));
        assertThrows(NullPointerException.class, () -> profiles.all(null));
        assertThrows(NullPointerException.class, () -> profiles.exists(null));
        assertThrows(NullPointerException.class, () -> profiles.isSetUp(null));
    }

    @Test
    void aCharacterIdNeedsAPlayerAndAProfile() {
        assertThrows(NullPointerException.class, () -> new CharacterId(null, "Default"));
        assertThrows(NullPointerException.class, () -> new CharacterId(ALICE, null));
    }
}
