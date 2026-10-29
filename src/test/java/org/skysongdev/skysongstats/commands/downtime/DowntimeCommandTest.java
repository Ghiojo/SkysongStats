package org.skysongdev.skysongstats.commands.downtime;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.Utils.StatsManager;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.service.downtime.CharacterKey;
import org.skysongdev.skysongstats.service.downtime.DowntimeChangeListener;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;
import org.skysongdev.skysongstats.service.downtime.InMemoryDowntimeRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs /downtime against an in-memory repository. The plugin is loaded so
 * {@code Utils} and the profile lists work; its own listeners are removed so
 * adding test players does not touch the (missing) database.
 */
class DowntimeCommandTest {

    private ServerMock server;
    private SkysongStats plugin;
    private InMemoryDowntimeRepository repository;
    private DowntimeService service;
    private DowntimeCommand downtime;
    private Command command;
    private ConsoleCommandSenderMock console;
    private PlayerMock staff;
    private PlayerMock alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongStats.class);
        HandlerList.unregisterAll(plugin);
        StatsManager.statsProfileList = new ArrayList<>();
        ProfileManager.activeProfiles = new ArrayList<>();

        repository = new InMemoryDowntimeRepository();
        service = new DowntimeService(repository, DowntimeSettings.defaults(), DowntimeChangeListener.NONE);
        downtime = new DowntimeCommand(service);
        command = plugin.getCommand("downtime");
        console = server.getConsoleSender();

        staff = server.addPlayer("Staff");
        staff.setOp(true);
        alice = server.addPlayer("Alice");
        giveProfiles(staff, "Default");
        giveProfiles(alice, "Default", "Mage");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static void giveProfiles(PlayerMock player, String... profiles) {
        for (String profile : profiles) {
            StatsManager.statsProfileList.add(new PlayerStats(player.getUniqueId().toString(), profile));
        }
        ProfileManager.activeProfiles.add(new ProfileIndex(player.getUniqueId().toString(), profiles[0]));
    }

    private void run(CommandSender sender, String... args) {
        downtime.onCommand(sender, command, "downtime", args);
    }

    private static String next(PlayerMock player) {
        Component message = player.nextComponentMessage();
        return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    private static String next(ConsoleCommandSenderMock console) {
        Component message = console.nextComponentMessage();
        return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    private UUID aliceId() {
        return alice.getUniqueId();
    }

    // Issue 32

    @Test
    void addToProfileChangesThatProfileOnly() {
        service.removeCharacter(aliceId(), "Mage", 3);
        run(staff, "add", "2", "Alice", "Mage");

        assertEquals(2, service.getCharacter(aliceId(), "Mage"));
        assertEquals(3, service.getCharacter(aliceId(), "Default"));
        assertEquals(3, service.getGlobal(aliceId()));
        assertTrue(next(staff).contains("Added 2 downtime to player Alice's profile Mage, now 2."));
    }

    @Test
    void addGlobalIsCappedAndSaysSo() {
        run(staff, "add", "10", "Alice", "global");
        assertEquals(9, repository.globals.get(aliceId()));
        assertTrue(next(staff).contains("Added 6 downtime to player Alice's global downtime, now 9. (capped at the maximum)"));
    }

    @Test
    void addToUnknownProfileFails() {
        run(staff, "add", "1", "Alice", "Rogue");
        assertTrue(next(staff).contains("Profile Rogue not found for player Alice."));
        assertTrue(repository.characters.isEmpty());
    }

    @Test
    void addRejectsBadAmounts() {
        run(staff, "add", "0", "Alice", "global");
        run(staff, "add", "-2", "Alice", "global");
        run(staff, "add", "two", "Alice", "global");
        for (int i = 0; i < 3; i++) {
            assertTrue(next(staff).contains("Amount must be a whole number of 1 or more."));
        }
        assertTrue(repository.globals.isEmpty());
    }

    @Test
    void unknownPlayerFails() {
        run(staff, "add", "1", "Nobody", "global");
        assertTrue(next(staff).contains("Player Nobody not found or has no profile."));
    }

    @Test
    void worksOnOfflinePlayers() {
        alice.disconnect();
        run(console, "add", "1", "Alice", "global");
        assertEquals(4, service.getGlobal(aliceId()));
    }

    @Test
    void addWithoutPermissionIsRefused() {
        run(alice, "add", "1", "Alice", "global");
        assertTrue(next(alice).contains("You do not have permission"));
        assertTrue(repository.globals.isEmpty());
    }

    // Answer 1 and issue 37

    @Test
    void removeStopsAtZeroAndConfirmsProfileRemovals() {
        run(staff, "remove", "5", "Alice", "Mage");
        assertEquals(0, service.getCharacter(aliceId(), "Mage"));
        assertTrue(next(staff).contains("Successfully removed 3 downtime from Alice's Mage profile downtime, now 0. (stopped at 0)"));
    }

    @Test
    void removeWithoutPoolUsesThePreference() {
        run(staff, "remove", "1", "Alice");
        assertEquals(2, service.getCharacter(aliceId(), "Default"));
        assertEquals(3, service.getGlobal(aliceId()));

        service.setPreference(aliceId(), true);
        run(staff, "remove", "1", "Alice");
        assertEquals(2, service.getGlobal(aliceId()));
    }

    // Issue 35

    @Test
    void viewOtherPlayerShowsTheirDowntime() {
        service.addGlobal(aliceId(), 2);
        run(staff, "view", "Alice");
        assertEquals("Alice's Downtime:", next(staff));
        assertEquals("- Global Downtime: 5", next(staff));
        assertEquals("- Character Downtime: 3", next(staff));
        assertEquals("- Active Downtime Spending Preference: Character", next(staff));
    }

    @Test
    void viewOtherPlayerWithoutPermissionStopsAfterTheError() {
        PlayerMock bob = server.addPlayer("Bob");
        giveProfiles(bob, "Default");
        run(bob, "view", "Alice");
        assertTrue(next(bob).contains("You do not have permission to view other players' downtime."));
        assertNull(next(bob));
    }

    @Test
    void viewOwnDowntime() {
        run(alice, "view");
        assertEquals("Alice's Downtime:", next(alice));
    }

    @Test
    void viewFromConsoleNeedsAPlayer() {
        run(console, "view");
        assertTrue(next(console).contains("Usage: /downtime view <player>"));
    }

    // Issue 34

    @Test
    void preferenceToggleIsSaved() {
        run(alice, "preference");
        assertTrue(repository.spendsGlobal.get(aliceId()));
        assertTrue(next(alice).contains("set to Global"));
        run(alice, "preference");
        assertEquals(false, repository.spendsGlobal.get(aliceId()));
    }

    // Issues 31 and 33

    @Test
    void resetAllFromConsoleSavesAndCoversPlayersWithoutRows() {
        repository.characters.put(new CharacterKey(aliceId(), "Mage"), 0);
        service.load();

        run(console, "resetall");

        assertEquals(3, repository.characters.get(new CharacterKey(aliceId(), "Mage")));
        assertEquals(3, repository.characters.get(new CharacterKey(aliceId(), "Default")));
        assertEquals(6, repository.globals.get(aliceId()));
        assertEquals(6, repository.globals.get(staff.getUniqueId()));
        assertTrue(next(console).contains("All downtimes have been reset successfully."));
    }

    @Test
    void tabCompletesProfilesForAdd() {
        List<String> options = downtime.onTabComplete(staff, command, "downtime", new String[]{"add", "1", "Alice", ""});
        assertEquals(List.of("global", "Default", "Mage"), options);
    }

    @Test
    void tabCompletesNamesIgnoringCase() {
        List<String> options = downtime.onTabComplete(staff, command, "downtime", new String[]{"view", "al"});
        assertEquals(List.of("Alice"), options);
    }

    // Messages come from messages.yml

    private void editMessages(String from, String to) throws IOException {
        Path file = new File(plugin.getDataFolder(), "messages.yml").toPath();
        Files.writeString(file, Files.readString(file).replace(from, to));
        plugin.reloadSettings();
    }

    @Test
    void usesTheTextFromMessagesYml() throws IOException {
        editMessages("Successfully removed {amount} downtime from {player}'s {profile} profile downtime, now {total}.\"",
                "Quitado {amount} de {profile} de {player}: {total}\"");
        run(staff, "remove", "1", "Alice", "Mage");
        assertEquals("[SkysongStats] Quitado 1 de Mage de Alice: 2", next(staff));
    }

    @Test
    void translatesThePreferenceWords() throws IOException {
        editMessages("preference-global: \"Global\"", "preference-global: \"Global (servidor)\"");
        run(alice, "preference");
        assertTrue(next(alice).contains("set to Global (servidor)."));
    }

    @Test
    void consoleGetsTheUsageMessages() {
        run(console);
        assertEquals("[SkysongStats] Invalid arguments! Use /downtime <subcommand>", next(console));
        run(console, "nope");
        assertEquals("[SkysongStats] Unknown subcommand!", next(console));
        run(console, "preference");
        assertEquals("[SkysongStats] Only players can set downtime preferences!", next(console));
    }
}
