package org.skysongdev.skysongstats;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.api.event.StatChangeEvent;
import org.skysongdev.skysongstats.character.AfflictionFixtures;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The plugin applies what injuries and conditions do to stats: on awards and
 * removals, with StatChangeEvent, and after /ssreload changes the effects.
 */
class AfflictionEffectsTest {
    private static final ProfileKey CHARACTER = new ProfileKey(UUID.randomUUID(), "Default");

    private SkysongStats plugin;
    private StatService stats;
    private AfflictionService afflictions;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        ServerMock server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        write("conditions.yml", AfflictionFixtures.CONDITIONS_FILE);
        write("injuries.yml", AfflictionFixtures.INJURIES_FILE);
        assertEquals(List.of(), plugin.reloadSettings());
        stats = plugin.getStatService();
        afflictions = plugin.getAfflictionService();
        stats.createCharacter(CHARACTER);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onStat(StatChangeEvent event) {
                events.add(event.getChange().stat().id() + " " + event.getChange().before() + " -> "
                        + event.getChange().after() + " (" + event.getCause() + ")");
            }
        }, plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void write(String file, String text) throws IOException {
        Files.writeString(new File(plugin.getDataFolder(), file).toPath(), text, StandardCharsets.UTF_8);
    }

    private int value(String name) {
        return stats.value(CHARACTER, name).orElseThrow();
    }

    @Test
    void anInjuryChangesStatsUntilItIsRemoved() {
        plugin.getChangeCauses().during(ChangeCause.COMMAND, () -> afflictions.awardInjury(CHARACTER, "broken-leg"));
        assertEquals(-2, value("dex"));
        assertEquals(List.of("dex 0 -> -2 (COMMAND)"), events);

        afflictions.removeInjury(CHARACTER, "broken-leg");
        assertEquals(0, value("dex"));
    }

    @Test
    void conditionLevelsMultiplyPerLevelModifiers() {
        afflictions.awardCondition(CHARACTER, "bleeding", 3);
        assertEquals(-3, value("str"));
        afflictions.awardInjury(CHARACTER, "cut");
        assertEquals(-3, value("str"));
        afflictions.removeCondition(CHARACTER, "bleeding");
        assertEquals(-2, value("str"));
    }

    @Test
    void aReloadAppliesChangedEffectsToEveryone() throws IOException {
        afflictions.awardInjury(CHARACTER, "bruise");
        write("injuries.yml", AfflictionFixtures.INJURIES_FILE.replace(
                "bruise: {name: \"Bruise\", severity: minor}",
                "bruise: {name: \"Bruise\", severity: minor, effects: {modifiers: \"str+4\"}}"));

        assertEquals(List.of(), plugin.reloadSettings());

        assertEquals(4, value("str"));
    }
}
