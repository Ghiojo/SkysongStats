package org.skysongdev.skysongstats.commands.misc;

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
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** /ssreload must apply changed downtime limits and messages without a restart. */
class ReloadCommandTest {

    private ServerMock server;
    private SkysongStats plugin;
    private Command command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongStats.class);
        HandlerList.unregisterAll(plugin);
        command = plugin.getCommand("ssreload");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void editConfig() {
        plugin.getConfig().set("downtime.character-max", 4);
        plugin.getConfig().set("downtime.global-start", 2);
        plugin.getConfig().set("downtime.global-recovery", 5);
        plugin.getConfig().set("downtime.global-max", 12);
        plugin.saveConfig();
    }

    @Test
    void startsWithTheBundledDefaults() {
        assertEquals(DowntimeSettings.defaults(), plugin.getDowntimeService().getSettings());
    }

    @Test
    void reloadAppliesNewDowntimeLimits() {
        editConfig();
        new ReloadCommand().onCommand(server.getConsoleSender(), command, "ssreload", new String[0]);
        assertEquals(new DowntimeSettings(4, 2, 5, 12), plugin.getDowntimeService().getSettings());
    }

    @Test
    void reloadWithoutPermissionChangesNothing() {
        editConfig();
        PlayerMock player = server.addPlayer("Player");
        new ReloadCommand().onCommand(player, command, "ssreload", new String[0]);
        assertEquals(DowntimeSettings.defaults(), plugin.getDowntimeService().getSettings());
    }

    private static String plain(Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    @Test
    void reloadTellsThePlayerWithTheMessageFromMessagesYml() {
        PlayerMock player = server.addPlayer("Admin");
        player.setOp(true);
        new ReloadCommand().onCommand(player, command, "ssreload", new String[0]);
        assertEquals("[SkysongStats] Config Reloaded!", plain(player.nextComponentMessage()));
    }

    @Test
    void reloadWithoutPermissionSendsTheNoPermissionMessage() {
        PlayerMock player = server.addPlayer("Player");
        new ReloadCommand().onCommand(player, command, "ssreload", new String[0]);
        assertEquals("[SkysongStats] You don't have permission to use this command!", plain(player.nextComponentMessage()));
    }

    @Test
    void reloadReadsEditedMessages() throws IOException {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        Files.writeString(file.toPath(), Files.readString(file.toPath()).replace("Config Reloaded!", "Recargado"));
        PlayerMock player = server.addPlayer("Admin");
        player.setOp(true);
        new ReloadCommand().onCommand(player, command, "ssreload", new String[0]);
        assertEquals("[SkysongStats] Recargado", plain(player.nextComponentMessage()));
    }
}
