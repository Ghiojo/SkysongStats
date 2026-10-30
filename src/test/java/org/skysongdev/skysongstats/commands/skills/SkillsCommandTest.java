package org.skysongdev.skysongstats.commands.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.events.SkillChangeEvent;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * /skills on the plugin's SkillService and the default skills.yml, run from the
 * console and by players. Alice and Bob have an active "Default" character; Bob
 * is not an op.
 */
class SkillsCommandTest {
    private ServerMock server;
    private SkysongStats plugin;
    private SkillService skills;
    private PluginCommand command;
    private ConsoleCommandSenderMock console;
    private PlayerMock alice;
    private PlayerMock bob;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        skills = plugin.getSkillService();
        command = plugin.getCommand("skills");
        console = server.getConsoleSender();
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        ProfileManager.activeProfiles = new ArrayList<>();
        for (PlayerMock player : List.of(alice, bob)) {
            ProfileManager.activeProfiles.add(new ProfileIndex(player.getUniqueId().toString(), "Default"));
            plugin.getStatService().createCharacter(character(player));
            skills.createCharacter(character(player));
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ProfileKey character(PlayerMock player) {
        return new ProfileKey(player.getUniqueId(), "Default");
    }

    private int xp(PlayerMock player, String skill) {
        return skills.xp(character(player), skills.config().skills().get(skill).orElseThrow()).orElseThrow();
    }

    private static String plain(Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }

    private void run(CommandSender sender, String... args) {
        command.getExecutor().onCommand(sender, command, "skills", args);
    }

    // The first line the console gets for this command.
    private String console(String... args) {
        run(console, args);
        return plain(console.nextComponentMessage());
    }

    public static final class FiredEvents implements Listener {
        final List<SkillChangeEvent> events = new ArrayList<>();

        @EventHandler
        public void onSkillChange(SkillChangeEvent event) {
            events.add(event);
        }
    }

    @Test
    void unknownSubcommand() {
        assertEquals("[SkysongStats] Incorrect Arguments! Usage: /skill (addxp/setxp/view) (skill) (value)",
                console("nope"));
    }

    @Test
    void consoleMustNameAPlayer() {
        assertEquals("[SkysongStats] You must specify a player if you are running this command on console!",
                console("addxp", "mining", "5"));
        assertEquals("[SkysongStats] You must specify a player if you are running this command on console!",
                console("setxp", "mining", "5"));
    }

    @Test
    void consoleIsToldWhenThePlayerIsMissing() {
        assertEquals("[SkysongStats] Player Nobody not found!", console("addxp", "mining", "5", "Nobody"));
    }

    @Test
    void usageNamesTheRightSubcommand() {
        assertEquals("[SkysongStats] Too Little arguments! (Usage: /skill addxp (skill) (xp) (player (optional)))",
                console("addxp", "mining"));
        assertEquals("[SkysongStats] Too Little arguments! (Usage: /skill setxp (skill) (xp) (player (optional)))",
                console("setxp", "mining"));
    }

    @Test
    void consoleAddsXpAndThePlayerIsTold() {
        assertEquals("[SkysongStats] Added 20 XP to Alice's Mining skill!", console("addxp", "mining", "20", "Alice"));
        assertEquals("[SkysongStats] You have gained 20 XP in your Mining skill!", plain(alice.nextComponentMessage()));
        console("addxp", "mining", "5", "Alice");
        assertEquals(25, xp(alice, "mining"));
    }

    @Test
    void consoleSetsXp() {
        assertEquals("[SkysongStats] Set Mining XP to 50 for Alice", console("setxp", "mining", "50", "Alice"));
        assertEquals(50, xp(alice, "mining"));
    }

    @Test
    void skillsAreFoundByIdOrAliasInAnyCase() {
        console("setxp", "MIN", "7", "Alice");
        console("setxp", "T&E", "8", "Alice");
        console("setxp", "Farming", "9", "Alice");
        assertEquals(7, xp(alice, "mining"));
        assertEquals(8, xp(alice, "tande"));
        assertEquals(9, xp(alice, "farming"));
    }

    @Test
    void unknownSkillsAndBadXpChangeNothing() {
        assertEquals("[SkysongStats] Invalid Skill!", console("addxp", "juggling", "5", "Alice"));
        assertEquals("[SkysongStats] Invalid XP!", console("addxp", "mining", "-5", "Alice"));
        assertEquals("[SkysongStats] Invalid XP!", console("setxp", "mining", "lots", "Alice"));
        assertEquals(0, xp(alice, "mining"));
    }

    @Test
    void addingXpToYourselfSendsOneMessage() {
        alice.setOp(true);
        run(alice, "addxp", "mining", "3");
        assertEquals("[SkysongStats] Added 3 XP to Alice's Mining skill!", plain(alice.nextComponentMessage()));
        assertNull(alice.nextComponentMessage());
        assertEquals(3, xp(alice, "mining"));
    }

    @Test
    void addxpNeedsItsPermission() {
        run(bob, "addxp", "mining", "3", "Alice");
        assertEquals("[SkysongStats] You don't have permission to use this command!", plain(bob.nextComponentMessage()));
        assertEquals(0, xp(alice, "mining"));
    }

    @Test
    void setxpWorksWithTheSetxpPermission() {
        bob.addAttachment(plugin, "skysongstats.setxp", true);
        run(bob, "setxp", "mining", "40", "Alice");
        assertEquals(40, xp(alice, "mining"));
    }

    @Test
    void everyXpChangeFiresASkillChangeEvent() {
        FiredEvents fired = new FiredEvents();
        server.getPluginManager().registerEvents(fired, plugin);
        console("addxp", "mining", "16", "Alice");
        assertEquals(1, fired.events.size());
        assertEquals("mining", fired.events.get(0).getChange().skill().id());
        assertEquals(16, fired.events.get(0).getChange().after());
    }

    @Test
    void consoleCannotOpenTheBook() {
        assertEquals("[SkysongStats] Only players can use this command!", console());
        assertEquals("[SkysongStats] Only players can use this command!", console("view", "Alice"));
    }

    @Test
    void tabCompletesSubcommandsSkillIdsAndPlayers() {
        assertEquals(List.of("addxp"), command.tabComplete(console, "skills", new String[]{"a"}));
        assertEquals(List.of("mining", "mistgathering", "medical", "martial", "metalworking").stream().sorted().toList(),
                command.tabComplete(console, "skills", new String[]{"addxp", "m"})
                        .stream().sorted().toList());
        assertEquals(List.of("Bob"),
                command.tabComplete(console, "skills", new String[]{"setxp", "mining", "1", "B"}));
        assertEquals(List.of("Alice"),
                command.tabComplete(console, "skills", new String[]{"view", "A"}));
    }
}
