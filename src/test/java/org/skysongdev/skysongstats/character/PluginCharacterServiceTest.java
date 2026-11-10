package org.skysongdev.skysongstats.character;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.storage.PluginCharacterRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests for {@link PluginCharacterService}, with an in-memory repository and
 * a lifecycle that records what it is asked to do.
 */
class PluginCharacterServiceTest {
    private static final String NPCS = "SkysongNPCs";
    private static final String SHOPS = "Shops";
    private static final ProfileKey GUARD = new ProfileKey(UUID.randomUUID(), "npc");
    private static final ProfileKey MERCHANT = new ProfileKey(UUID.randomUUID(), "npc");
    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "Default");

    private final Map<ProfileKey, String> saved = new LinkedHashMap<>();
    private final Set<ProfileKey> existing = new HashSet<>();
    private final List<String> lifecycleCalls = new ArrayList<>();
    private PluginCharacterService service;

    private final class MemoryRepository implements PluginCharacterRepository {
        @Override
        public void createTables() {
            // Nothing to create in memory.
        }

        @Override
        public Map<ProfileKey, String> loadAll() {
            return Map.copyOf(saved);
        }

        @Override
        public void save(ProfileKey character, String owner) {
            saved.put(character, owner);
        }

        @Override
        public void delete(ProfileKey character) {
            saved.remove(character);
        }
    }

    private final class RecordingLifecycle implements PluginCharacterService.CharacterLifecycle {
        @Override
        public boolean exists(ProfileKey character) {
            return existing.contains(character);
        }

        @Override
        public void create(ProfileKey character, Map<String, Integer> startingValues) {
            if (startingValues.containsKey("luck")) {
                throw new IllegalArgumentException("Unknown stat: luck");
            }
            existing.add(character);
            lifecycleCalls.add("create " + character.player() + " " + startingValues);
        }

        @Override
        public void delete(ProfileKey character) {
            existing.remove(character);
            lifecycleCalls.add("delete " + character.player());
        }
    }

    @BeforeEach
    void setUp() {
        existing.add(ALICE);
        service = new PluginCharacterService(new MemoryRepository(), new RecordingLifecycle());
        service.load();
    }

    @Test
    void createMakesTheCharacterAndSavesItsOwner() {
        assertTrue(service.create(NPCS, GUARD, Map.of("str", 4)));

        assertEquals(List.of("create " + GUARD.player() + " {str=4}"), lifecycleCalls);
        assertEquals(Map.of(GUARD, NPCS), saved);
        assertTrue(service.isPluginCharacter(GUARD));
        assertEquals(Optional.of(NPCS), service.owner(GUARD));
    }

    @Test
    void anIdThatIsAlreadyAPlayersProfileIsRefused() {
        assertFalse(service.create(NPCS, ALICE, Map.of()));

        assertEquals(List.of(), lifecycleCalls);
        assertEquals(Map.of(), saved);
    }

    @Test
    void anIdThatIsAlreadyAPluginCharacterIsRefused() {
        service.create(NPCS, GUARD, Map.of());

        assertFalse(service.create(SHOPS, GUARD, Map.of()));
        assertEquals(Optional.of(NPCS), service.owner(GUARD));
    }

    @Test
    void anUnknownStatSavesNoOwner() {
        Map<String, Integer> values = Map.of("luck", 3);

        assertThrows(IllegalArgumentException.class, () -> service.create(NPCS, GUARD, values));
        assertFalse(service.isPluginCharacter(GUARD));
        assertEquals(Map.of(), saved);
    }

    @Test
    void playersProfilesHaveNoOwner() {
        assertFalse(service.isPluginCharacter(ALICE));
        assertEquals(Optional.empty(), service.owner(ALICE));
    }

    @Test
    void ownedByListsOnlyThatPluginsCharactersOldestFirst() {
        service.create(NPCS, MERCHANT, Map.of());
        service.create(SHOPS, new ProfileKey(UUID.randomUUID(), "npc"), Map.of());
        service.create(NPCS, GUARD, Map.of());

        assertEquals(List.of(MERCHANT, GUARD), service.ownedBy(NPCS));
        assertEquals(List.of(), service.ownedBy("Nobody"));
    }

    @Test
    void theOwnerDeletesItsCharacter() {
        service.create(NPCS, GUARD, Map.of());

        assertTrue(service.delete(NPCS, GUARD));

        assertEquals("delete " + GUARD.player(), lifecycleCalls.getLast());
        assertFalse(service.isPluginCharacter(GUARD));
        assertEquals(Map.of(), saved);
    }

    @Test
    void anotherPluginCannotDeleteIt() {
        service.create(NPCS, GUARD, Map.of());

        assertFalse(service.delete(SHOPS, GUARD));

        assertTrue(service.isPluginCharacter(GUARD));
        assertEquals(1, lifecycleCalls.size());
    }

    @Test
    void aPlayersProfileCannotBeDeletedThroughIt() {
        assertFalse(service.delete(NPCS, ALICE));

        assertTrue(existing.contains(ALICE));
        assertEquals(List.of(), lifecycleCalls);
    }

    @Test
    void loadBringsBackTheSavedCharacters() {
        service.create(NPCS, GUARD, Map.of());

        PluginCharacterService restarted = new PluginCharacterService(new MemoryRepository(), new RecordingLifecycle());
        restarted.load();

        assertEquals(Optional.of(NPCS), restarted.owner(GUARD));
    }

    @Test
    void playerCharactersLeavesThePluginCharactersOut() {
        service.create(NPCS, GUARD, Map.of());

        assertEquals(Set.of(ALICE), service.playerCharacters(Set.of(ALICE, GUARD)));
    }
}
