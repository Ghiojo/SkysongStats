package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.CharacterManager;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.api.Ancestries;
import org.skysongdev.skysongstats.api.Ancestry;
import org.skysongdev.skysongstats.api.AncestryAdjustment;
import org.skysongdev.skysongstats.api.AncestryChange;
import org.skysongdev.skysongstats.api.AncestryChoice;
import org.skysongdev.skysongstats.api.AncestryPicks;
import org.skysongdev.skysongstats.api.CharacterAncestry;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.SheetField;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongstats.api.event.AncestryChangeEvent;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.setup.AncestryChoicesMenu;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ancestries() of the running plugin with the shipped ancestries.yml. Alice is online and
 * playing "Default"; the NPC plugin owns a guard with no player.
 */
class AncestriesApiTest {
    private ServerMock server;
    private SkysongStats plugin;
    private PlayerMock player;
    private CharacterId alice;
    private CharacterProfile sheet;
    private Ancestries ancestries;
    private final List<AncestryChangeEvent> events = new ArrayList<>();

    public final class Recorder implements Listener {
        @EventHandler
        public void onChange(AncestryChangeEvent event) {
            events.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        player = server.addPlayer("Alice");
        player.addAttachment(plugin, "skysongstats.ancestry.choose", true);
        alice = new CharacterId(player.getUniqueId(), "Default");
        plugin.getStatService().createCharacter(new ProfileKey(alice.player(), alice.profile()));
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(player.getUniqueId().toString(), "Default")));
        sheet = new CharacterProfile(player.getUniqueId().toString(), "Default");
        CharacterManager.characterProfiles = new ArrayList<>(List.of(sheet));
        ancestries = SkysongStatsApi.get().ancestries();
        server.getPluginManager().registerEvents(new Recorder(), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Ancestry ancestry(String id) {
        return ancestries.ancestry(id).orElseThrow();
    }

    private int value(CharacterId character, String stat) {
        return plugin.getStatService().value(new ProfileKey(character.player(), character.profile()), stat).orElseThrow();
    }

    @Test
    void theDefinitionsAreTheFiles() {
        assertEquals(List.of("dwarf", "elf", "human", "goblin", "orc", "giant", "dragonborn"),
                ancestries.all().stream().map(Ancestry::id).toList());
        Ancestry dwarf = ancestry("dwarf");
        assertEquals("Dwarf", dwarf.name());
        assertEquals(List.of(new AncestryAdjustment("con", 1), new AncestryAdjustment("spd", -1)), dwarf.adjustments());
        assertFalse(dwarf.hasChoices());
        assertEquals("", dwarf.permission());

        AncestryChoice.Stats versatile = assertInstanceOf(AncestryChoice.Stats.class, ancestry("human").choices().get(0));
        assertEquals(2, versatile.picks());
        assertTrue(versatile.anyStat());
        AncestryChoice.Packages upbringing = assertInstanceOf(AncestryChoice.Packages.class,
                ancestry("orc").choices().get(0));
        assertEquals(List.of(new AncestryAdjustment("str", 2)), upbringing.options().get(0).adjustments());
        assertEquals("IRON_AXE", upbringing.options().get(0).icon());
        assertTrue(((AncestryChoice.Stats) ancestry("giant").choices().get(0)).stack());
        assertEquals("skysong.ancestry.dragonborn", ancestry("dragonborn").permission());
    }

    @Test
    void ancestriesAreFoundByIdOrName() {
        assertEquals("giant", ancestries.ancestry("HALF-GIANT").orElseThrow().id());
        assertEquals("giant", ancestries.ancestry(" Giant ").orElseThrow().id());
        assertTrue(ancestries.ancestry("merfolk").isEmpty());
    }

    @Test
    void mayPickFollowsThePermission() {
        assertTrue(ancestries.mayPick(player, ancestry("dwarf")));
        assertFalse(ancestries.mayPick(player, ancestry("dragonborn")));
        player.addAttachment(plugin, "skysong.ancestry.dragonborn", true);
        assertTrue(ancestries.mayPick(player, ancestry("dragonborn")));
    }

    @Test
    void problemsUseTheChoicesPageRules() {
        Ancestry human = ancestry("human");
        assertEquals(List.of(), ancestries.problems(human, AncestryPicks.builder().stats(0, "str", "con").build()));
        assertFalse(ancestries.problems(human, AncestryPicks.builder().stats(0, "str").build()).isEmpty());
        assertFalse(ancestries.problems(human, AncestryPicks.none()).isEmpty());
        assertEquals(List.of(), ancestries.problems(ancestry("orc"), AncestryPicks.builder().option(0, 1).build()));
        assertFalse(ancestries.problems(ancestry("orc"), AncestryPicks.builder().option(0, 5).build()).isEmpty());
    }

    @Test
    void setWithPicksGivesEverythingAndFiresTheEvent() {
        AncestryChange change = ancestries.set(alice, ancestry("orc"), AncestryPicks.builder().option(0, 1).build())
                .orElseThrow();

        assertTrue(change.before().isEmpty());
        CharacterAncestry orc = change.after().orElseThrow();
        assertEquals("orc", orc.id());
        assertFalse(orc.choicesPending());
        assertEquals(List.of(new CharacterAncestry.Given("dex", 1, 0), new CharacterAncestry.Given("spd", 1, 0)),
                orc.adjustments());
        assertEquals(1, orc.total("DEX"));
        assertEquals(1, value(alice, "dex"));
        assertEquals("Orc", sheet.getAncestry());
        assertEquals(1, events.size());
        assertEquals(ChangeCause.API, events.get(0).getCause());
        assertEquals(alice, events.get(0).getCharacter());
        assertEquals(orc, events.get(0).getAfter().orElseThrow());
        assertEquals(orc, ancestries.of(alice).orElseThrow());
    }

    @Test
    void statAliasesWorkInPicks() {
        Ancestry human = ancestry("human");
        ancestries.set(alice, human, AncestryPicks.builder().stats(0, "Strength", "con").build());

        assertEquals(1, value(alice, "str"));
    }

    @Test
    void invalidPicksThrowAndChangeNothing() {
        Ancestry human = ancestry("human");
        AncestryPicks one = AncestryPicks.builder().stats(0, "str").build();

        assertThrows(IllegalArgumentException.class, () -> ancestries.set(alice, human, one));
        assertTrue(ancestries.of(alice).isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void setWithoutPicksLeavesChoicesPendingThenChooseMakesThem() {
        ancestries.set(alice, ancestry("human"));
        assertTrue(ancestries.of(alice).orElseThrow().choicesPending());

        CharacterAncestry chosen = ancestries.choose(alice, AncestryPicks.builder().stats(0, "dex", "con").build())
                .orElseThrow().after().orElseThrow();

        assertFalse(chosen.choicesPending());
        assertEquals(1, value(alice, "dex"));
        assertTrue(ancestries.choose(alice, AncestryPicks.none()).isEmpty(), "nothing pending any more");
    }

    @Test
    void rechooseAndRemove() {
        ancestries.set(alice, ancestry("giant"), AncestryPicks.builder().stats(0, "str").build());
        assertEquals(2, value(alice, "str"));

        CharacterAncestry pending = ancestries.rechoose(alice).orElseThrow().after().orElseThrow();
        assertTrue(pending.choicesPending());
        assertEquals(List.of(new CharacterAncestry.Given("str", 1, -1)), pending.adjustments());
        assertTrue(pending.adjustments().get(0).fixed());
        assertEquals(1, value(alice, "str"));

        AncestryChange removed = ancestries.remove(alice).orElseThrow();
        assertTrue(removed.after().isEmpty());
        assertEquals(0, value(alice, "str"));
        assertEquals(CharacterProfile.NOT_SET, sheet.getAncestry());
        assertTrue(ancestries.remove(alice).isEmpty());
        assertTrue(ancestries.rechoose(alice).isEmpty());
        assertEquals(3, events.size());
    }

    @Test
    void anUnknownCharacterGetsNothing() {
        CharacterId nobody = new CharacterId(UUID.randomUUID(), "Default");

        assertTrue(ancestries.set(nobody, ancestry("dwarf")).isEmpty());
        assertTrue(ancestries.of(nobody).isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void pluginCharactersCanHaveAncestries() {
        Plugin npcs = MockBukkit.createMockPlugin("Npcs");
        CharacterId guard = new CharacterId(UUID.randomUUID(), "npc");
        assertTrue(SkysongStatsApi.get().characters().create(npcs, guard));

        ancestries.set(guard, ancestry("human"));
        ancestries.choose(guard, AncestryPicks.builder().stats(0, "str", "foc").build());

        assertEquals(1, value(guard, "foc"));
        assertFalse(ancestries.of(guard).orElseThrow().choicesPending());
    }

    @Test
    void openChoicesOpensThePageForPendingChoices() {
        assertFalse(ancestries.openChoices(player));
        ancestries.set(alice, ancestry("elf"));
        events.clear();

        assertTrue(ancestries.openChoices(player));
        AncestryChoicesMenu page = assertInstanceOf(AncestryChoicesMenu.class,
                player.getOpenInventory().getTopInventory().getHolder());
        page.click(player, 10, ClickType.LEFT);
        page.click(player, AncestryChoicesMenu.CONFIRM_SLOT, ClickType.LEFT);

        assertFalse(ancestries.of(alice).orElseThrow().choicesPending());
        assertEquals(ChangeCause.API, events.get(0).getCause());
    }

    @Test
    void theSheetFieldIsLockedWhileTheCharacterHasAnAncestry() {
        ancestries.set(alice, ancestry("dwarf"));

        assertThrows(IllegalStateException.class,
                () -> SkysongStatsApi.get().sheets().set(alice, SheetField.ANCESTRY, "Elf"));
        assertEquals("Dwarf", sheet.getAncestry());
    }

    @Test
    void picksAreImmutableValues() {
        AncestryPicks picks = AncestryPicks.builder().stats(0, "str").option(1, 2).stats(1, "dex").build();

        assertEquals(List.of("dex"), picks.stats(1).orElseThrow());
        assertTrue(picks.option(1).isEmpty(), "a later pick replaces the earlier one");
        assertEquals(Set.of(0, 1), picks.choices());
        assertEquals(picks, AncestryPicks.builder().stats(1, "dex").stats(0, "str").build());
        assertSame(AncestryPicks.none(), AncestryPicks.builder().build());
        assertThrows(IllegalArgumentException.class, () -> AncestryPicks.builder().option(-1, 0));
    }

    @Test
    void callsFromAnotherThreadAreRefused() {
        AncestriesImpl offThread = new AncestriesImpl(plugin::getAncestryService, plugin.getStatService(),
                plugin::getAncestryPrompt, new MainThread(() -> false), plugin.getChangeCauses());

        assertThrows(IllegalStateException.class, () -> offThread.of(alice));
        assertThrows(IllegalStateException.class, () -> offThread.set(alice, ancestry("dwarf")));
        assertEquals(7, offThread.all().size(), "the definitions are safe from any thread");
    }
}
