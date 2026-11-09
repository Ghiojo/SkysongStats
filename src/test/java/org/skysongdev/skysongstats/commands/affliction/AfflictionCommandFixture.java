package org.skysongdev.skysongstats.commands.affliction;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.OfflineSkysongStats;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.character.AfflictionFixtures;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The plugin with the shared affliction fixtures as its injuries.yml and
 * conditions.yml, and the default messages.yml. Alice and Bob have an active
 * "Default" character; neither is an op.
 */
abstract class AfflictionCommandFixture {
    protected SkysongStats plugin;
    protected AfflictionService afflictions;
    protected ConsoleCommandSenderMock console;
    protected PlayerMock alice;
    protected PlayerMock bob;
    private PluginCommand command;

    protected abstract String commandName();

    @BeforeEach
    void startPlugin() {
        ServerMock server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        // MockBukkit doesn't add plugin.yml's permissions, so their defaults wouldn't apply.
        plugin.getPluginMeta().getPermissions().forEach(server.getPluginManager()::addPermission);
        write("conditions.yml", AfflictionFixtures.CONDITIONS_FILE);
        write("injuries.yml", AfflictionFixtures.INJURIES_FILE);
        assertEquals(List.of(), plugin.reloadSettings());
        afflictions = plugin.getAfflictionService();
        command = plugin.getCommand(commandName());
        console = server.getConsoleSender();
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        ProfileManager.activeProfiles = new ArrayList<>();
        for (PlayerMock player : List.of(alice, bob)) {
            ProfileManager.activeProfiles.add(new ProfileIndex(player.getUniqueId().toString(), "Default"));
            plugin.getStatService().createCharacter(character(player));
        }
    }

    @AfterEach
    void stopPlugin() {
        MockBukkit.unmock();
    }

    private void write(String file, String text) {
        try {
            Files.writeString(new File(plugin.getDataFolder(), file).toPath(), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static ProfileKey character(PlayerMock player) {
        return new ProfileKey(player.getUniqueId(), "Default");
    }

    protected static String plain(Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }

    protected void run(CommandSender sender, String... args) {
        command.getExecutor().onCommand(sender, command, commandName(), args);
    }

    // The first line the console gets for this command.
    protected String console(String... args) {
        run(console, args);
        return plain(console.nextComponentMessage());
    }

    protected static String next(PlayerMock player) {
        return plain(player.nextComponentMessage());
    }

    protected static List<String> all(PlayerMock player) {
        List<String> lines = new ArrayList<>();
        for (Component line = player.nextComponentMessage(); line != null; line = player.nextComponentMessage()) {
            lines.add(plain(line));
        }
        return lines;
    }

    protected List<String> complete(CommandSender sender, String... args) {
        return command.tabComplete(sender, commandName(), args);
    }
}
