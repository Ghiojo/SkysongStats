package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.Focus;
import org.skysongdev.skysongstats.api.Proficiencies;
import org.skysongdev.skysongstats.api.Proficiency;
import org.skysongdev.skysongstats.api.SkillChange;
import org.skysongdev.skysongstats.api.SkysongStatsApi;
import org.skysongdev.skysongstats.api.StatAction;
import org.skysongdev.skysongstats.api.StatChange;
import org.skysongdev.skysongstats.api.TierChange;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.api.event.ProficiencyFocusChangeEvent;
import org.skysongdev.skysongstats.api.event.ProficiencyTierChangeEvent;
import org.skysongdev.skysongstats.api.event.ProfileUpdateEvent;
import org.skysongdev.skysongstats.api.event.SkillChangeEvent;
import org.skysongdev.skysongstats.api.event.StatChangeEvent;
import org.skysongdev.skysongstats.api.event.SkysongStatsReloadEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The events other plugins get, fired by the running plugin.
class ApiEventsTest {
    private SkysongStats plugin;
    private CharacterId alice;
    private Fired fired;

    public static final class Fired implements Listener {
        final List<SkillChangeEvent> skills = new ArrayList<>();
        final List<StatChangeEvent> stats = new ArrayList<>();
        final List<ProficiencyTierChangeEvent> tiers = new ArrayList<>();
        final List<ProficiencyFocusChangeEvent> focuses = new ArrayList<>();
        int reloads;

        @EventHandler
        public void onSkillChange(SkillChangeEvent event) {
            skills.add(event);
        }

        @EventHandler
        public void onStatChange(StatChangeEvent event) {
            stats.add(event);
        }

        @EventHandler
        public void onTier(ProficiencyTierChangeEvent event) {
            tiers.add(event);
        }

        @EventHandler
        public void onFocus(ProficiencyFocusChangeEvent event) {
            focuses.add(event);
        }

        @EventHandler
        public void onReload(SkysongStatsReloadEvent event) {
            reloads++;
        }
    }

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        // The join listeners would write downtime, and there is no database.
        HandlerList.unregisterAll(plugin);
        PlayerMock player = server.addPlayer("Alice");
        alice = new CharacterId(player.getUniqueId(), "Default");
        plugin.getSkillService().createCharacter(new ProfileKey(alice.player(), alice.profile()));
        plugin.getStatService().createCharacter(new ProfileKey(alice.player(), alice.profile()));
        plugin.getProficiencyService().createCharacter(new ProfileKey(alice.player(), alice.profile()));
        fired = new Fired();
        server.getPluginManager().registerEvents(fired, plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void anApiWriteFiresTheSkillEventWithCauseApi() {
        SkillChange change = SkysongStatsApi.get().skills().addXp(alice, "mining", 16).orElseThrow();

        assertEquals(1, fired.skills.size());
        assertEquals(change, fired.skills.getFirst().getChange());
        assertEquals(ChangeCause.API, fired.skills.getFirst().getCause());
    }

    @Test
    void aWriteOutsideTheApiIsACommand() {
        plugin.getSkillService().addXp(new ProfileKey(alice.player(), alice.profile()),
                plugin.getSkillService().config().skills().get("mining").orElseThrow(), 3);

        assertEquals(ChangeCause.COMMAND, fired.skills.getFirst().getCause());
    }

    @Test
    void theSkillEventCarriesApiTypesOnly() {
        SkysongStatsApi.get().skills().setXp(alice, "mining", 40);

        SkillChange change = fired.skills.getFirst().getChange();
        assertEquals(alice, change.character());
        assertEquals(SkysongStatsApi.get().skills().find("mining").orElseThrow(), change.skill());
        assertEquals(40, change.after());
    }

    @Test
    void anApiStatWriteFiresTheStatEventWithCauseApi() {
        StatChange change = SkysongStatsApi.get().stats().apply(alice, "str", StatAction.ADD, 2).orElseThrow();

        assertEquals(1, fired.stats.size());
        assertEquals(change, fired.stats.getFirst().getChange());
        assertEquals(ChangeCause.API, fired.stats.getFirst().getCause());
    }

    @Test
    void apiProficiencyWritesFireTheProficiencyEventsWithApiTypes() {
        Proficiencies proficiencies = SkysongStatsApi.get().proficiencies();
        Proficiency axes = proficiencies.find("axes").orElseThrow();

        proficiencies.setFocus(alice, axes, false);
        TierChange change = proficiencies.setTier(alice, axes, proficiencies.findTier("adept").orElseThrow())
                .orElseThrow();

        ProficiencyFocusChangeEvent focus = fired.focuses.getFirst();
        assertEquals(alice, focus.getCharacter());
        assertEquals(Optional.empty(), focus.getBefore());
        assertEquals(Optional.of(new Focus(axes, 0)), focus.getAfter());
        assertEquals(ChangeCause.API, focus.getCause());
        assertEquals(change, fired.tiers.getFirst().getChange());
        assertEquals(ChangeCause.API, fired.tiers.getFirst().getCause());
    }

    @Test
    void ssreloadFiresTheReloadEventOnce() {
        plugin.reloadSettings();

        assertEquals(1, fired.reloads);
    }

    @Test
    void theProfileUpdateEventCarriesBothCharacters() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Bob");
        CharacterId before = new CharacterId(player.getUniqueId(), "Default");
        CharacterId after = new CharacterId(player.getUniqueId(), "Mage");

        ProfileUpdateEvent event = new ProfileUpdateEvent(player, before, after);

        assertSame(player, event.getPlayer());
        assertEquals(Optional.of(before), event.getPrevious());
        assertEquals(after, event.getCharacter());
        assertEquals(Optional.empty(), new ProfileUpdateEvent(player, null, after).getPrevious());
        assertThrows(NullPointerException.class, () -> new ProfileUpdateEvent(player, before, null));
    }
}
