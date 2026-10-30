package org.skysongdev.skysongstats.commands.character;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.event.HandlerList;
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
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.database.ProfileIndex;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** /char messages come from messages.yml; character fields keep their colours. */
class CharacterCommandTest {
    private static final String BORDER = "|====================<<₪>>====================|";

    private ServerMock server;
    private CharacterCommand character;
    private Command command;
    private PlayerMock alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        SkysongStats plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        character = new CharacterCommand();
        command = plugin.getCommand("char");
        alice = server.addPlayer("Alice");
        String uuid = alice.getUniqueId().toString();
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(uuid, "Default")));
        CharacterManager.characterProfiles = new ArrayList<>(List.of(new CharacterProfile(
                uuid, "Default", "<red>Aria", "30", "Female", "Elf", "she/her", "A quiet <bold>mage</bold>")));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private String next() {
        Component message = alice.nextComponentMessage();
        return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    @Test
    void showsTheCharacterSheet() {
        character.onCommand(alice, command, "char", new String[0]);
        assertEquals(BORDER, next());
        assertEquals("                Alice' s Character Sheet:", next());
        assertEquals("", next());
        assertEquals("Name: Aria", next());
        assertEquals("Age: 30", next());
        assertEquals("Gender: Female(she/her)", next());
        assertEquals("Ancestry: Elf", next());
        assertEquals("Description:", next());
        assertEquals("A quiet mage ", next());
        assertEquals(BORDER, next());
        assertNull(next());
    }

    @Test
    void keepsTheColoursPlayersWrote() {
        character.onCommand(alice, command, "char", new String[0]);
        for (int i = 0; i < 3; i++) {
            next();
        }
        Component name = alice.nextComponentMessage();
        assertEquals(MiniMessage.miniMessage().deserialize("<#D0530C><bold>Name:</bold> <#f8e886><red>Aria").compact(), name.compact());
    }

    @Test
    void wrapsTheDescriptionAtAboutSixtyCharacters() {
        String description = "word ".repeat(30).trim();
        List<String> lines = ViewCharacter.wrapDescription(description);
        assertEquals(3, lines.size());
        assertEquals("word ".repeat(12), lines.get(0));
    }

    @Test
    void consoleMustNameAPlayer() {
        new ViewCharacter().onCommand(server.getConsoleSender(), command, "char", new String[0]);
        assertEquals("You must specify a player to view their character!",
                PlainTextComponentSerializer.plainText().serialize(server.getConsoleSender().nextComponentMessage()));
    }

    @Test
    void consoleWithoutAPlayerIsAskedForOne() {
        character.onCommand(server.getConsoleSender(), command, "char", new String[0]);
        assertEquals("You must specify a player to view their character!", nextConsole());
    }

    @Test
    void consoleCanViewAPlayersSheet() {
        character.onCommand(server.getConsoleSender(), command, "char", new String[]{"view", "Alice"});
        assertEquals(BORDER, nextConsole());
        assertEquals("                Alice' s Character Sheet:", nextConsole());
    }

    @Test
    void consoleCannotSetFields() {
        character.onCommand(server.getConsoleSender(), command, "char", new String[]{"set", "name", "Aria"});
        assertEquals("Only players can use this command!", nextConsole());
    }

    private String nextConsole() {
        Component message = server.getConsoleSender().nextComponentMessage();
        return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
    }

    @Test
    void tooLongNameIsRefused() {
        character.onCommand(alice, command, "char", new String[]{"set", "name", "x".repeat(101)});
        assertEquals("[SkysongStats] Name is too long!", next());
    }

    @Test
    void unknownSubcommand() {
        character.onCommand(alice, command, "char", new String[]{"nope"});
        assertEquals("[SkysongStats] Invalid subcommand!", next());
    }
}
