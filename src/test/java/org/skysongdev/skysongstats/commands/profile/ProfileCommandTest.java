package org.skysongdev.skysongstats.commands.profile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.Utils.StatsManager;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.database.ProfileIndex;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** /profile messages come from messages.yml. Only paths that do not write to the database are run. */
class ProfileCommandTest {
    private ProfileCommand profile;
    private Command command;
    private PlayerMock alice;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        SkysongStats plugin = MockBukkit.load(SkysongStats.class);
        HandlerList.unregisterAll(plugin);
        profile = new ProfileCommand();
        command = plugin.getCommand("profile");
        alice = server.addPlayer("Alice");
        String uuid = alice.getUniqueId().toString();
        StatsManager.statsProfileList = new ArrayList<>(List.of(new PlayerStats(uuid, "Default"), new PlayerStats(uuid, "Mage")));
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(uuid, "Default")));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private String run(String... args) {
        profile.onCommand(alice, command, "profile", args);
        Component message = alice.nextComponentMessage();
        return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    @Test
    void viewShowsTheCurrentProfile() {
        assertEquals("[SkysongStats] Your current profile is Default", run("view"));
    }

    @Test
    void createRefusesADuplicateName() {
        assertEquals("[SkysongStats] You already have a profile with this name!", run("create", "Mage"));
    }

    @Test
    void createRefusesALongName() {
        assertEquals("[SkysongStats] Profile name is too long! (Max 20 characters)", run("create", "x".repeat(21)));
    }

    @Test
    void switchToAMissingProfile() {
        assertEquals("[SkysongStats] You have no profile with that name!", run("switch", "Rogue"));
    }

    @Test
    void deleteTheActiveProfileIsRefused() {
        assertEquals("[SkysongStats] You cannot delete the profile you are currently on, switch profiles before doing so!", run("delete", "Default"));
    }

    @Test
    void usageMessagesNameTheRightSubcommand() {
        assertEquals("[SkysongStats] Too Little arguments! (Usage: /profile switch (name))", run("switch"));
        assertEquals("[SkysongStats] Too Little arguments! (Usage: /profile delete (name))", run("delete"));
        assertEquals("[SkysongStats] Incorrect Arguments! Usage: /profile (create/switch/delete) (profile)", run("nope"));
    }
}
