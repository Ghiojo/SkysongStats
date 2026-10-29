package org.skysongdev.skysongstats.commands.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerSkills;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** /skill messages and the skills book come from messages.yml. */
class SkillsCommandTest {
    private SkillsCommand skills;
    private Command command;
    private ConsoleCommandSenderMock console;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        SkysongStats plugin = MockBukkit.load(SkysongStats.class);
        HandlerList.unregisterAll(plugin);
        skills = new SkillsCommand();
        command = plugin.getCommand("skills");
        console = server.getConsoleSender();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private String run(String... args) {
        skills.onCommand(console, command, "skill", args);
        Component message = console.nextComponentMessage();
        return message == null ? null : plain(message);
    }

    @Test
    void unknownSubcommand() {
        assertEquals("[SkysongStats] Incorrect Arguments! Usage: /skill (addxp/setxp/view) (skill) (value)", run("nope"));
    }

    @Test
    void consoleMustNameAPlayer() {
        assertEquals("[SkysongStats] You must specify a player if you are running this command on console!", run("addxp", "mining", "5"));
        assertEquals("[SkysongStats] You must specify a player if you are running this command on console!", run("setxp", "mining", "5"));
    }

    @Test
    void consoleIsToldWhenThePlayerIsMissing() {
        assertEquals("[SkysongStats] Player Nobody not found!", run("addxp", "mining", "5", "Nobody"));
    }

    @Test
    void usageNamesTheRightSubcommand() {
        assertEquals("[SkysongStats] Too Little arguments! (Usage: /skill addxp (skill) (xp) (player (optional)))", run("addxp", "mining"));
        assertEquals("[SkysongStats] Too Little arguments! (Usage: /skill setxp (skill) (xp) (player (optional)))", run("setxp", "mining"));
    }

    @Test
    void bookListsEverySkillWithItsLevel() {
        PlayerSkills playerSkills = new PlayerSkills(UUID.randomUUID().toString());
        playerSkills.setSkill(Utils.Skills.ANIMAL_HANDLING, 20);
        List<Component> pages = ViewSkills.pages(playerSkills);

        assertEquals(3, pages.size());
        String gathering = plain(pages.get(0));
        assertTrue(gathering.startsWith("Gathering Skills:\n\nAnimal Handling: Novice(20)\nFarming: Unskilled(0)\n"), gathering);
        assertEquals(8, gathering.split("\n", -1).length);
        assertTrue(plain(pages.get(1)).startsWith("Crafting Skills:\n\n"));
        assertTrue(plain(pages.get(2)).startsWith("Service Skills:\n\n"));
    }
}
