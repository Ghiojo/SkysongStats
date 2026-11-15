package org.skysongdev.skysongstats.apiimpl;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.api.ShortNameAssignment;
import org.skysongdev.skysongstats.api.Stat;
import org.skysongdev.skysongstats.api.StatType;
import org.skysongdev.skysongstats.api.event.StatShortNameChangeEvent;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;
import org.skysongdev.skysongstats.storage.UnsavedStatRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Short names through the API: reading them on {@link Stat}, assigning and taking
 * them off, and the event.
 */
class StatsImplShortNamesTest {
    private static final String STATS = """
            stats:
              str: {name: Strength, aliases: [strength], short-name: STR}
              dex: {name: Dexterity}
            """;

    private final List<StatShortNameChangeEvent> fired = new ArrayList<>();
    private ServerMock server;
    private Plugin npcs;
    private StatService service;
    private StatsImpl stats;

    private static StatDefinitions definitions(String yaml) {
        YamlConfiguration file = new YamlConfiguration();
        try {
            file.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return StatsConfigLoader.load(file, name -> true).stats();
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        npcs = MockBukkit.createMockPlugin("Npcs");
        service = new StatService(definitions(STATS), new UnsavedStatRepository(line -> { }), change -> { });
        stats = new StatsImpl(service, new MainThread(() -> true), new ChangeCauses(), this::record);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void record(Event event) {
        fired.add((StatShortNameChangeEvent) event);
    }

    private String shortName(String stat) {
        return stats.find(stat).orElseThrow().shortName();
    }

    @Test
    void aStatCarriesItsShortNameFromStatsYmlOrItsName() {
        assertEquals("STR", shortName("str"));
        assertEquals("Dexterity", shortName("dex"));
        assertEquals("str", stats.find("STR").orElseThrow().id());
    }

    @Test
    void theOldStatConstructorUsesTheNameAsShortName() {
        Stat luck = new Stat("luck", "Luck", "", List.of(), StatType.VALUE, null, null);
        assertEquals("Luck", luck.shortName());
        assertNotEquals(luck, new Stat("luck", "Luck", "", List.of(), StatType.VALUE, null, null, "LCK"));
    }

    @Test
    void assigningShowsEverywhereAndFiresAnEvent() {
        ShortNameAssignment agility = stats.assignShortName(npcs, "dex", "Agi").orElseThrow();
        assertEquals(npcs, agility.owner());
        assertEquals("dex", agility.statId());
        assertEquals("Agi", agility.shortName());
        assertTrue(agility.isAssigned());
        assertTrue(agility.isActive());

        assertEquals("Agi", shortName("dex"));
        assertEquals("dex", stats.find("agi").orElseThrow().id(), "commands take it");
        assertEquals("Agi", service.definitions().get("dex").orElseThrow().display().shortName());
        assertEquals(List.of(agility), stats.shortNameAssignments());

        assertEquals(1, fired.size());
        assertEquals("dex", fired.get(0).getStat().id());
        assertEquals("Dexterity", fired.get(0).getOldShortName());
        assertEquals("Agi", fired.get(0).getNewShortName());
    }

    @Test
    void unassigningBringsBackTheOneBefore() {
        ShortNameAssignment first = stats.assignShortName(npcs, "str", "Mgt").orElseThrow();
        ShortNameAssignment second = stats.assignShortName(npcs, "str", "Pow").orElseThrow();
        assertFalse(first.isActive());
        second.unassign();
        second.unassign();
        assertFalse(second.isAssigned());
        assertFalse(second.isActive());
        assertEquals("Mgt", shortName("str"));
        first.unassign();
        assertEquals("STR", shortName("str"));
        assertEquals(List.of("STR", "Mgt", "Pow", "Mgt", "STR"), List.of(fired.get(0).getOldShortName(),
                fired.get(0).getNewShortName(), fired.get(1).getNewShortName(), fired.get(2).getNewShortName(),
                fired.get(3).getNewShortName()));
        assertTrue(stats.shortNameAssignments().isEmpty());
    }

    @Test
    void refusedAssignmentsChangeNothing() {
        assertEquals(Optional.empty(), stats.assignShortName(npcs, "dex", "strength"));
        assertEquals(Optional.empty(), stats.assignShortName(npcs, "dex", " "));
        assertEquals(Optional.empty(), stats.assignShortName(npcs, "luck", "Lk"));
        Stat gone = new Stat("luck", "Luck", "", List.of(), StatType.VALUE, null, null);
        assertEquals(Optional.empty(), stats.assignShortName(npcs, gone, "Lk"));
        assertTrue(fired.isEmpty());
        assertTrue(stats.shortNameAssignments().isEmpty());
    }

    @Test
    void aDisabledPluginCantAssignAndLosesItsAssignments() {
        stats.assignShortName(npcs, "dex", "Agi").orElseThrow();
        server.getPluginManager().disablePlugin(npcs);
        assertThrows(IllegalStateException.class, () -> stats.assignShortName(npcs, "str", "Mgt"));

        stats.onPluginDisable(new PluginDisableEvent(npcs));
        assertEquals("Dexterity", shortName("dex"));
        assertTrue(stats.shortNameAssignments().isEmpty());
        assertEquals(2, fired.size());
    }

    @Test
    void assignmentsSurviveAReload() {
        stats.assignShortName(npcs, "dex", "Agi").orElseThrow();
        service.setDefinitions(definitions(STATS + "  con: {}\n"));
        assertEquals("Agi", shortName("dex"));
        assertEquals("Dexterity", service.configDefinitions().get("dex").orElseThrow().display().shortName());
    }

    @Test
    void writingFromAnotherThreadIsRefused() {
        StatsImpl offThread = new StatsImpl(service, new MainThread(() -> false), new ChangeCauses());
        assertThrows(IllegalStateException.class, () -> offThread.assignShortName(npcs, "dex", "Agi"));
        assertThrows(IllegalStateException.class, offThread::shortNameAssignments);
    }
}
