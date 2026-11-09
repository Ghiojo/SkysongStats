package org.skysongdev.skysongstats.commands.misc;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** /ssimport as plugin.yml and the plugin set it up. */
class ImportCommandTest {
    private PluginCommand command;
    private ConsoleCommandSenderMock console;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        SkysongStats plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        command = plugin.getCommand("ssimport");
        console = server.getConsoleSender();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private String next() {
        return PlainTextComponentSerializer.plainText().serialize(console.nextComponentMessage());
    }

    @Test
    void needsTheSourceToImportFrom() {
        command.getExecutor().onCommand(console, command, "ssimport", new String[0]);
        assertEquals("[SkysongStats] Usage: /ssimport chattweaks", next());
        command.getExecutor().onCommand(console, command, "ssimport", new String[]{"elsewhere"});
        assertEquals("[SkysongStats] Usage: /ssimport chattweaks", next());
    }

    @Test
    void looksForSkysongChatTweaksInTheConfiguredFolder() {
        command.getExecutor().onCommand(console, command, "ssimport", new String[]{"ChatTweaks"});
        String message = next();
        assertTrue(message.startsWith("[SkysongStats] There is no SkysongChatTweaks config at "), message);
        assertTrue(message.contains("SkysongChatTweaks"), message);
    }

    @Test
    void completesTheSource() {
        assertEquals(List.of("chattweaks"), ((ImportCommand) command.getExecutor()).onTabComplete(console, command, "ssimport",
                new String[]{"ch"}));
        assertEquals(List.of(), ((ImportCommand) command.getExecutor()).onTabComplete(console, command, "ssimport",
                new String[]{"x"}));
    }

    @Test
    void needsItsOwnPermission() {
        assertEquals("skysongstats.import", command.getPermission());
    }
}
