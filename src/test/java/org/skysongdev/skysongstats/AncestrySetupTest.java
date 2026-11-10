package org.skysongdev.skysongstats;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.Utils.CharacterManager;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.StatSetup;
import org.skysongdev.skysongstats.setup.AncestryChoicesMenu;
import org.skysongdev.skysongstats.setup.AncestrySetupMenu;
import org.skysongdev.skysongstats.setup.SetupScreen;
import org.skysongdev.skysongstats.setup.SkillSetupMenu;
import org.skysongdev.skysongstats.setup.StatSetupMenu;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAncestry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /setup with the shipped ancestries.yml: the ancestry list comes first, an
 * ancestry with choices opens its choices page, the stat screen starts with the
 * ancestry applied, and finishing gives the character its ancestry. Alice has an
 * active "Default" profile and may use /setup and /char; she isn't an op, so
 * ancestries with a permission are hidden from her until she gets it.
 */
class AncestrySetupTest {
    private ServerMock server;
    private SkysongStats plugin;
    private PlayerMock alice;
    private ProfileKey character;
    private SetupProfile setup;
    private CharacterProfile sheet;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        alice = server.addPlayer("Alice");
        alice.addAttachment(plugin, "skysongstats.setup", true);
        alice.addAttachment(plugin, "skysongstats.char", true);
        String uuid = alice.getUniqueId().toString();
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(uuid, "Default")));
        setup = new SetupProfile(uuid, "Default", false);
        ProfileManager.setupProfiles = new ArrayList<>(List.of(setup));
        sheet = new CharacterProfile(uuid, "Default");
        CharacterManager.characterProfiles = new ArrayList<>(List.of(sheet));
        character = new ProfileKey(alice.getUniqueId(), "Default");
        plugin.getStatService().createCharacter(character);
        plugin.getSkillService().createCharacter(character);
        plugin.getProficiencyService().createCharacter(character);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private <T> T screen(Class<T> type) {
        return assertInstanceOf(type, alice.getOpenInventory().getTopInventory().getHolder());
    }

    private int value(String stat) {
        return plugin.getStatService().value(character, stat).orElseThrow();
    }

    // Spends the 7 points: 4 on str (slot 9), 3 on spd (slot 17), then picks the
    // first two skills.
    private void finishStatsAndSkills() {
        StatSetupMenu stats = screen(StatSetupMenu.class);
        for (int i = 0; i < 4; i++) {
            stats.click(alice, 9, ClickType.LEFT);
        }
        for (int i = 0; i < 3; i++) {
            stats.click(alice, 17, ClickType.LEFT);
        }
        stats.click(alice, StatSetup.CONFIRM_SLOT, ClickType.LEFT);
        SkillSetupMenu skills = screen(SkillSetupMenu.class);
        skills.click(alice, 10, ClickType.LEFT);
        skills.click(alice, 11, ClickType.LEFT);
        skills.click(alice, SkillSetupMenu.CONFIRM_SLOT, ClickType.LEFT);
        Inventory top = alice.getOpenInventory().getTopInventory();
        if (top != null && top.getHolder() instanceof SetupScreen open) {
            fail("expected /setup to be finished, but " + open + " is open");
        }
    }

    @Test
    void anAncestryWithoutChoicesGoesStraightToTheStatScreen() {
        alice.performCommand("setup");
        screen(AncestrySetupMenu.class).click(alice, 10, ClickType.LEFT);

        // The dwarf's con +1 and spd -1 already show.
        StatSetupMenu stats = screen(StatSetupMenu.class);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(
                stats.getInventory().getItem(13).getItemMeta().displayName()).contains("1"));
    }

    @Test
    void finishingGivesTheAncestryOnTopOfThePoints() {
        alice.performCommand("setup");
        screen(AncestrySetupMenu.class).click(alice, 10, ClickType.LEFT);
        finishStatsAndSkills();

        assertTrue(setup.isSetUp());
        assertEquals(4, value("str"));
        assertEquals(1, value("con"));
        assertEquals(8, value("spd")); // 6 + 3 points - 1 dwarf
        assertEquals(70, value("hp"), "HP fills to the max the ancestry's con gives");
        StoredAncestry dwarf = plugin.getAncestryService().of(character).orElseThrow();
        assertEquals("dwarf", dwarf.ancestry());
        assertFalse(dwarf.choicesPending());
        assertEquals("Dwarf", sheet.getAncestry());
    }

    @Test
    void anAncestryWithChoicesOpensItsChoicesPageFirst() {
        alice.performCommand("setup");
        screen(AncestrySetupMenu.class).click(alice, 12, ClickType.LEFT);
        AncestryChoicesMenu choices = screen(AncestryChoicesMenu.class);
        // Human: +1 to two stats of any; pick str and con.
        choices.click(alice, 10, ClickType.LEFT);
        choices.click(alice, 12, ClickType.LEFT);
        choices.click(alice, AncestryChoicesMenu.CONFIRM_SLOT, ClickType.LEFT);
        finishStatsAndSkills();

        assertEquals(5, value("str"));
        assertEquals(1, value("con"));
        assertEquals(9, value("spd"));
        assertEquals("Human", sheet.getAncestry());
        assertEquals(2, plugin.getAncestryService().of(character).orElseThrow().adjustments().size());
    }

    @Test
    void backOnTheChoicesPageReturnsToTheList() {
        alice.performCommand("setup");
        screen(AncestrySetupMenu.class).click(alice, 12, ClickType.LEFT);
        screen(AncestryChoicesMenu.class).click(alice, 0, ClickType.LEFT);
        screen(AncestrySetupMenu.class);
    }

    @Test
    void ancestriesWithAPermissionAreHiddenWithoutIt() {
        alice.performCommand("setup");
        assertNull(screen(AncestrySetupMenu.class).getInventory().getItem(16));
        alice.closeInventory();

        alice.addAttachment(plugin, "skysong.ancestry.dragonborn", true);
        alice.performCommand("setup");
        assertNotNull(screen(AncestrySetupMenu.class).getInventory().getItem(16));
    }

    @Test
    void abandoningSetupGivesNoAncestry() {
        alice.performCommand("setup");
        screen(AncestrySetupMenu.class).click(alice, 10, ClickType.LEFT);
        alice.closeInventory();

        assertTrue(plugin.getAncestryService().of(character).isEmpty());
        assertEquals(0, value("con"));
        assertFalse(setup.isSetUp());
    }

    @Test
    void withNoAncestriesSetupStartsWithTheStats() throws IOException {
        Files.writeString(new File(plugin.getDataFolder(), "ancestries.yml").toPath(), "ancestries: {}\n");
        new ReloadCommand().onCommand(server.getConsoleSender(), plugin.getCommand("ssreload"), "ssreload", new String[0]);

        alice.performCommand("setup");
        screen(StatSetupMenu.class);
        finishStatsAndSkills();
        assertTrue(plugin.getAncestryService().of(character).isEmpty());
        assertEquals(CharacterProfile.NOT_SET, sheet.getAncestry());
    }

    @Test
    void aCharacterWithAnAncestryCantRewriteTheField() {
        alice.performCommand("setup");
        screen(AncestrySetupMenu.class).click(alice, 10, ClickType.LEFT);
        finishStatsAndSkills();
        while (alice.nextComponentMessage() != null) {
            // Skip the setup messages.
        }

        alice.performCommand("char set ancestry Elf");

        assertEquals("[SkysongStats] Your ancestry comes from your character's setup and can't be changed here.",
                PlainTextComponentSerializer.plainText().serialize(alice.nextComponentMessage()));
        assertEquals("Dwarf", sheet.getAncestry());
    }
}
