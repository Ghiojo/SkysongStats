package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Characters;
import org.skysongdev.skysongstats.api.Skill;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.api.StatAction;
import org.skysongdev.skysongstats.api.event.StatChangeEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * characters() of the running plugin: an NPC plugin creating, using and deleting its
 * NPCs' characters through the API.
 */
class CharactersImplTest {
    private static final String NPCS = "SkysongNPCs";
    private static final String SHOPS = "Shops";
    private static final String STR = "str";

    private ServerMock server;
    private SkysongStats plugin;
    private SkysongStatsApi api;
    private Characters characters;
    private Plugin npcs;
    private CharacterId guard;
    private final List<StatChangeEvent> statEvents = new ArrayList<>();

    public final class Recorder implements Listener {
        @EventHandler
        public void onStatChange(StatChangeEvent event) {
            statEvents.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        api = SkysongStatsApi.get();
        characters = api.characters();
        npcs = MockBukkit.createMockPlugin(NPCS);
        guard = new CharacterId(UUID.randomUUID(), "npc");
        server.getPluginManager().registerEvents(new Recorder(), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Stat stat(String id) {
        return api.stats().find(id).orElseThrow();
    }

    private int total(CharacterId character, String stat) {
        return api.stats().get(character, stat).orElseThrow().total();
    }

    @Test
    void aCreatedCharacterHasStatsAtTheGivenAndStartValues() {
        assertTrue(characters.create(npcs, guard, Map.of(stat(STR), 4, stat("con"), 2)));

        assertEquals(4, total(guard, STR));
        assertEquals(0, total(guard, "dex"));
        assertEquals(80, total(guard, "hp"));
        assertEquals(List.of(), statEvents);
    }

    @Test
    void theRestOfTheApiWorksOnIt() {
        characters.create(npcs, guard);
        Skill skill = api.skills().all().getFirst();

        assertTrue(api.stats().apply(guard, "hp", StatAction.DAMAGE, 5).isPresent());
        assertEquals(55, total(guard, "hp"));
        assertTrue(api.modifiers().add(guard, STR, 2).isPresent());
        assertEquals(2, total(guard, STR));
        assertEquals(0, api.skills().xp(guard, skill).orElseThrow());
        assertEquals(guard, statEvents.getFirst().getChange().character());
    }

    @Test
    void itIsOwnedByThePluginThatCreatedIt() {
        characters.create(npcs, guard);

        assertEquals(Optional.of(NPCS), characters.owner(guard));
        assertEquals(List.of(guard), characters.ownedBy(npcs));
        assertEquals(List.of(), characters.ownedBy(MockBukkit.createMockPlugin(SHOPS)));
    }

    @Test
    void itIsNotAPlayersProfile() {
        characters.create(npcs, guard);

        assertEquals(List.of(), api.profiles().all(guard.player()));
        assertFalse(api.profiles().exists(guard));
        assertFalse(api.profiles().isSetUp(guard));
        assertEquals(Optional.empty(), api.profiles().active(guard.player()));
    }

    @Test
    void aPlayersProfileHasNoOwnerAndCannotBeTaken() {
        CharacterId alice = new CharacterId(UUID.randomUUID(), "Default");
        plugin.getStatService().createCharacter(new ProfileKey(alice.player(), alice.profile()));

        assertEquals(Optional.empty(), characters.owner(alice));
        assertFalse(characters.create(npcs, alice));
        assertTrue(api.profiles().exists(alice));
    }

    @Test
    void anIdCanOnlyBeCreatedOnce() {
        characters.create(npcs, guard, Map.of(stat(STR), 4));

        assertFalse(characters.create(MockBukkit.createMockPlugin(SHOPS), guard));
        assertEquals(4, total(guard, STR));
        assertEquals(Optional.of(NPCS), characters.owner(guard));
    }

    @Test
    void theOwnerDeletesItAndItsData() {
        characters.create(npcs, guard);

        assertTrue(characters.delete(npcs, guard));

        assertTrue(api.stats().get(guard, STR).isEmpty());
        assertEquals(Optional.empty(), characters.owner(guard));
        assertEquals(List.of(), characters.ownedBy(npcs));
    }

    @Test
    void anotherPluginCannotDeleteIt() {
        characters.create(npcs, guard);

        assertFalse(characters.delete(MockBukkit.createMockPlugin(SHOPS), guard));
        assertTrue(api.stats().get(guard, STR).isPresent());
    }

    @Test
    void aPlayersProfileCannotBeDeleted() {
        CharacterId alice = new CharacterId(UUID.randomUUID(), "Default");
        plugin.getStatService().createCharacter(new ProfileKey(alice.player(), alice.profile()));

        assertFalse(characters.delete(npcs, alice));
        assertTrue(api.stats().get(alice, STR).isPresent());
    }

    @Test
    void theProfileNameMustFitTheDatabase() {
        CharacterId empty = new CharacterId(UUID.randomUUID(), "");
        CharacterId tooLong = new CharacterId(UUID.randomUUID(), "a".repeat(21));
        CharacterId longest = new CharacterId(UUID.randomUUID(), "a".repeat(20));

        assertThrows(IllegalArgumentException.class, () -> characters.create(npcs, empty));
        assertThrows(IllegalArgumentException.class, () -> characters.create(npcs, tooLong));
        assertTrue(characters.create(npcs, longest));
    }

    @Test
    void aDisabledPluginCannotCreateCharacters() {
        server.getPluginManager().disablePlugin(npcs);

        assertThrows(IllegalStateException.class, () -> characters.create(npcs, guard));
        assertEquals(Optional.empty(), characters.owner(guard));
    }

    @Test
    void charactersStayWhenTheirPluginDisables() {
        characters.create(npcs, guard);

        server.getPluginManager().disablePlugin(npcs);

        assertEquals(Optional.of(NPCS), characters.owner(guard));
        assertTrue(api.stats().get(guard, STR).isPresent());
    }

    @Test
    void ownedByCannotBeChanged() {
        characters.create(npcs, guard);
        List<CharacterId> owned = characters.ownedBy(npcs);

        assertThrows(UnsupportedOperationException.class, owned::clear);
    }

    @Test
    void callsFromAnotherThreadAreRefused() {
        CharactersImpl offThread = new CharactersImpl(plugin.getPluginCharacterService(),
                () -> plugin.getStatService().definitions(), new MainThread(() -> false));

        assertThrows(IllegalStateException.class, () -> offThread.create(npcs, guard));
        assertThrows(IllegalStateException.class, () -> offThread.delete(npcs, guard));
        assertThrows(IllegalStateException.class, () -> offThread.owner(guard));
        assertThrows(IllegalStateException.class, () -> offThread.ownedBy(npcs));
    }
}
