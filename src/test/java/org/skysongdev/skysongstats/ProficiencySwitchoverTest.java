package org.skysongdev.skysongstats;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.events.ProficiencyFocusChangeEvent;
import org.skysongdev.skysongstats.events.ProficiencyTierChangeEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plugin starts its proficiencies from proficiencies.yml: the service, the
 * generated tier nodes, the events and /ssreload. Alice has an active "Default"
 * profile. Tests have no MySQL, so proficiencies are kept in memory.
 */
class ProficiencySwitchoverTest {
    private ServerMock server;
    private SkysongStats plugin;
    private ProficiencyService proficiencies;
    private ProfileKey character;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        proficiencies = plugin.getProficiencyService();
        PlayerMock alice = server.addPlayer("Alice");
        String uuid = alice.getUniqueId().toString();
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(uuid, "Default")));
        ProfileManager.setupProfiles = new ArrayList<>(List.of(new SetupProfile(uuid, "Default", false)));
        character = new ProfileKey(alice.getUniqueId(), "Default");
        proficiencies.createCharacter(character);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void reload() {
        new ReloadCommand().onCommand(server.getConsoleSender(), plugin.getCommand("ssreload"), "ssreload", new String[0]);
    }

    private File file() {
        return new File(plugin.getDataFolder(), "proficiencies.yml");
    }

    @Test
    void theDefaultProficienciesAreLoaded() {
        assertEquals(28, proficiencies.config().proficiencies().all().size());
        assertEquals("untrained", proficiencies.tier(character,
                proficiencies.config().proficiencies().get("axes").orElseThrow()).id());
    }

    @Test
    void theTierNodesComeFromProficienciesYml() {
        assertNotNull(server.getPluginManager().getPermission("skysongstats.proficiency.axes.proficient"));
        assertNotNull(server.getPluginManager().getPermission("skysongstats.proficiency.performance.master"));
        assertNull(server.getPluginManager().getPermission("skysongstats.proficiency.axes.untrained"));
    }

    @Test
    void changesFireTheProficiencyEvents() {
        ProficiencyDefinition axes = proficiencies.config().proficiencies().get("axes").orElseThrow();
        List<Event> fired = new ArrayList<>();
        Listener recorder = new Listener() {
            @EventHandler
            public void onTier(ProficiencyTierChangeEvent event) {
                fired.add(event);
            }

            @EventHandler
            public void onFocus(ProficiencyFocusChangeEvent event) {
                fired.add(event);
            }
        };
        server.getPluginManager().registerEvents(recorder, plugin);

        proficiencies.changeFocus(character, axes);
        proficiencies.setTier(character, axes, proficiencies.config().tiers().get("adept").orElseThrow());

        assertEquals(2, fired.size());
        ProficiencyFocusChangeEvent focus = assertInstanceOf(ProficiencyFocusChangeEvent.class, fired.get(0));
        assertEquals("axes", focus.getChange().after().proficiency().id());
        ProficiencyTierChangeEvent tier = assertInstanceOf(ProficiencyTierChangeEvent.class, fired.get(1));
        assertTrue(tier.getChange().tierUp());
        assertEquals("adept", tier.getChange().after().id());
    }

    @Test
    void reloadingProficienciesYmlAppliesItsProficienciesAndNodes() throws IOException {
        Files.writeString(file().toPath(), Files.readString(file().toPath())
                .replace("  axes:            {name: \"Axes\",", "  hatchets:        {name: \"Hatchets\","));

        reload();

        assertTrue(proficiencies.config().proficiencies().get("hatchets").isPresent());
        assertTrue(proficiencies.config().proficiencies().get("axes").isEmpty());
        assertNotNull(server.getPluginManager().getPermission("skysongstats.proficiency.hatchets.adept"));
        assertNull(server.getPluginManager().getPermission("skysongstats.proficiency.axes.adept"));
    }

    @Test
    void aBrokenProficienciesYmlIsReportedAndTheOldProficienciesKept() throws IOException {
        Files.writeString(file().toPath(), "tiers: []\n");

        reload();

        // The first line says the rest of the config was reloaded.
        server.getConsoleSender().nextComponentMessage();
        String report = PlainTextComponentSerializer.plainText().serialize(server.getConsoleSender().nextComponentMessage());
        assertTrue(report.contains("proficiencies.yml"), report);
        assertTrue(proficiencies.config().proficiencies().get("axes").isPresent());
    }

    @Test
    void proficienciesOfASkillThatLeftSkillsYmlAreNotReloaded() throws IOException {
        File skills = new File(plugin.getDataFolder(), "skills.yml");
        String text = Files.readString(skills.toPath());
        Files.writeString(skills.toPath(), text.replace("  medical:\n", "  healing:\n").replace(" medical,", " healing,"));

        reload();

        assertTrue(plugin.getSkillService().config().skills().get("healing").isPresent());
        // Medicine still names the medical skill, so the proficiencies from before stay.
        assertEquals("medical", proficiencies.config().proficiencies().get("medicine").orElseThrow().skill());
    }
}
