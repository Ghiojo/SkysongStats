package org.skysongdev.skysongstats.commands.setup;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** /setup builds a character for the player who runs it, so the console is turned away. */
class SetupCommandTest {
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void consoleIsToldOnlyPlayersCanSetUp() {
        ServerMock server = MockBukkit.mock();
        SkysongStats plugin = OfflineSkysongStats.load();
        HandlerList.unregisterAll(plugin);
        ConsoleCommandSenderMock console = server.getConsoleSender();

        plugin.setupCommand.onCommand(console, plugin.getCommand("setup"), "setup", new String[0]);

        assertEquals("[SkysongStats] Only players can use this command!",
                PlainTextComponentSerializer.plainText().serialize(console.nextComponentMessage()));
    }
}
