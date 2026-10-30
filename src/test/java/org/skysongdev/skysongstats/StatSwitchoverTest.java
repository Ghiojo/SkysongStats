package org.skysongdev.skysongstats;

import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.commands.stat.StatAliasCommand;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.StatSetup;
import org.skysongdev.skysongstats.setup.SkillSetupMenu;
import org.skysongdev.skysongstats.setup.StatSetupMenu;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plugin runs its stat commands and /setup on StatService and stats.yml.
 * Alice is an op with an active "Default" profile.
 */
class StatSwitchoverTest {
    private ServerMock server;
    private SkysongStats plugin;
    private PlayerMock alice;
    private ProfileKey character;
    private SetupProfile setup;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongStats.class);
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        alice = server.addPlayer("Alice");
        alice.setOp(true);
        String uuid = alice.getUniqueId().toString();
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(uuid, "Default")));
        setup = new SetupProfile(uuid, "Default", false);
        ProfileManager.setupProfiles = new ArrayList<>(List.of(setup));
        character = new ProfileKey(alice.getUniqueId(), "Default");
        plugin.getStatService().createCharacter(character);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private int value(String stat) {
        return plugin.getStatService().value(character, stat).orElseThrow();
    }

    @Test
    void theCommandNamesInStatsYmlAreRegistered() {
        for (String label : List.of("heal", "addhp", "damage", "dhp", "reducehp", "rhp", "temphp", "ac", "potionsickness")) {
            assertInstanceOf(StatAliasCommand.class, server.getCommandMap().getCommand(label), label);
        }
    }

    @Test
    void healAndDamageChangeHp() {
        alice.performCommand("damage 15");
        assertEquals(45, value("hp"));
        alice.performCommand("heal 5");
        assertEquals(50, value("hp"));
        server.dispatchCommand(server.getConsoleSender(), "stats set str 2 Alice");
        assertEquals(2, value("str"));
    }

    @Test
    void anyPlayerCanSetAnyonesPotionSickness() {
        PlayerMock bob = server.addPlayer("Bob");
        bob.performCommand("potionsickness 3 Alice");
        assertEquals(3, value("potion"));
    }

    @Test
    void theGeneratedPermissionsAreDeclared() {
        assertNotNull(server.getPluginManager().getPermission("skysongstats.stat.hp.add.other"));
        assertNotNull(server.getPluginManager().getPermission("skysongstats.stat.hp.damage"));
    }

    @Test
    void reloadingStatsYmlSwapsTheCommandNames() throws IOException {
        File file = new File(plugin.getDataFolder(), "stats.yml");
        Files.writeString(file.toPath(), Files.readString(file.toPath()).replace("add: [heal, addhp]", "add: [mend]"));
        new ReloadCommand().onCommand(server.getConsoleSender(), plugin.getCommand("ssreload"), "ssreload", new String[0]);
        assertNull(server.getCommandMap().getCommand("heal"));
        assertInstanceOf(StatAliasCommand.class, server.getCommandMap().getCommand("mend"));
        alice.performCommand("mend 1");
        assertEquals(60, value("hp"));
    }

    @Test
    void setupOpensTheStatScreenAndConfirmingMovesToTheSkillPicker() {
        alice.performCommand("setup");
        StatSetupMenu menu = assertInstanceOf(StatSetupMenu.class, alice.getOpenInventory().getTopInventory().getHolder());
        for (int i = 0; i < 4; i++) {
            menu.click(alice, 9, ClickType.LEFT);
        }
        for (int i = 0; i < 3; i++) {
            menu.click(alice, 17, ClickType.LEFT);
        }
        menu.click(alice, StatSetup.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals(Map.of("str", 4, "dex", 0, "con", 0, "foc", 0, "spd", 9), setup.getChosenStats());
        assertInstanceOf(SkillSetupMenu.class, alice.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void finishingSetupSetsTheChosenStats() {
        setup.setChosenStats(Map.of("str", 4, "spd", 9, "removed", 3));
        setup.applyChosenStats(plugin.getStatService());
        assertEquals(4, value("str"));
        assertEquals(9, value("spd"));
    }

    @Test
    void theStatSheetIsShownFromMessagesYml() {
        alice.performCommand("stats");
        String first = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(alice.nextComponentMessage());
        assertTrue(first.startsWith("|===="), first);
    }
}
