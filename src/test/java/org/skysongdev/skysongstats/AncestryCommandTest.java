package org.skysongdev.skysongstats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.Utils.CharacterManager;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.event.ProfileUpdateEvent;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.database.ProfileIndex;
import org.skysongdev.skysongstats.setup.AncestryChoicesMenu;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAncestry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /ancestry with the shipped ancestries.yml. Staff is an op; Alice is a player
 * playing her "Default" character, with stats and a character sheet.
 */
class AncestryCommandTest {
    private ServerMock server;
    private SkysongStats plugin;
    private PlayerMock staff;
    private PlayerMock alice;
    private ProfileKey character;
    private CharacterProfile sheet;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = OfflineSkysongStats.load();
        // The join listeners need the old tables, which tests don't have.
        HandlerList.unregisterAll(plugin);
        staff = server.addPlayer("Staff");
        staff.setOp(true);
        alice = server.addPlayer("Alice");
        // Everyone has it on a server (plugin.yml); the test server doesn't read the defaults.
        alice.addAttachment(plugin, "skysongstats.ancestry.choose", true);
        String uuid = alice.getUniqueId().toString();
        ProfileManager.activeProfiles = new ArrayList<>(List.of(new ProfileIndex(uuid, "Default")));
        ProfileManager.setupProfiles = new ArrayList<>();
        sheet = new CharacterProfile(uuid, "Default");
        CharacterManager.characterProfiles = new ArrayList<>(List.of(sheet));
        character = new ProfileKey(alice.getUniqueId(), "Default");
        plugin.getStatService().createCharacter(character);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    // Every message the player got since the last call.
    private static List<String> messages(PlayerMock player) {
        List<String> messages = new ArrayList<>();
        for (Component message = player.nextComponentMessage(); message != null; message = player.nextComponentMessage()) {
            messages.add(plain(message));
        }
        return messages;
    }

    private int value(String stat) {
        return plugin.getStatService().value(character, stat).orElseThrow();
    }

    private StoredAncestry ancestry() {
        return plugin.getAncestryService().of(character).orElseThrow();
    }

    private AncestryChoicesMenu choicesPage() {
        return assertInstanceOf(AncestryChoicesMenu.class, alice.getOpenInventory().getTopInventory().getHolder());
    }

    // Human: +1 to two stats of any; picks str (slot 10) and con (slot 12).
    private void chooseStrAndCon() {
        alice.performCommand("ancestry choose");
        AncestryChoicesMenu page = choicesPage();
        page.click(alice, 10, ClickType.LEFT);
        page.click(alice, 12, ClickType.LEFT);
        page.click(alice, AncestryChoicesMenu.CONFIRM_SLOT, ClickType.LEFT);
    }

    @Test
    void setGivesTheAncestryAndFillsTheField() {
        int spd = value("spd");
        staff.performCommand("ancestry set Alice dwarf");

        assertEquals("dwarf", ancestry().ancestry());
        assertFalse(ancestry().choicesPending());
        assertEquals(1, value("con"));
        assertEquals(spd - 1, value("spd"));
        assertEquals("Dwarf", sheet.getAncestry());
        assertEquals(List.of("[SkysongStats] Alice's character Default is now Dwarf."), messages(staff));
        server.getScheduler().performOneTick();
        assertEquals(List.of("[SkysongStats] Your character's ancestry is now Dwarf."), messages(alice),
                "no choices, so no prompt");
    }

    @Test
    void settingTheSameAncestryAgainDoesNotAddItTwice() {
        staff.performCommand("ancestry set Alice dwarf");
        staff.performCommand("ancestry set Alice DWARF");

        assertEquals(1, value("con"));
    }

    @Test
    void setSwapsTheOldAncestryOff() {
        staff.performCommand("ancestry set Alice dwarf");
        staff.performCommand("ancestry set Alice giant");

        assertEquals(0, value("con"));
        assertEquals(1, value("str"));
        assertEquals("Half-giant", sheet.getAncestry());
    }

    @Test
    void setIgnoresTheAncestrysPermission() {
        staff.performCommand("ancestry set Alice dragonborn");

        assertEquals("dragonborn", ancestry().ancestry());
    }

    @Test
    void anAncestryWithChoicesLeavesThemPendingAndPromptsThePlayer() {
        staff.performCommand("ancestry set Alice human");

        assertTrue(ancestry().choicesPending());
        assertEquals(List.of("[SkysongStats] Alice's character Default is now Human; they still have to make its choices."),
                messages(staff));
        server.getScheduler().performOneTick();
        assertEquals(List.of("[SkysongStats] Your character's ancestry is now Human.",
                "[SkysongStats] Your ancestry, Human, has choices to make. [Choose]"), messages(alice));
    }

    @Test
    void chooseMakesThePendingChoices() {
        staff.performCommand("ancestry set Alice human");
        messages(alice);

        chooseStrAndCon();

        assertFalse(ancestry().choicesPending());
        assertEquals(1, value("str"));
        assertEquals(1, value("con"));
        assertEquals(List.of("[SkysongStats] Your Human choices are made."), messages(alice));
    }

    @Test
    void theChoicesPageHasNoBackButton() {
        staff.performCommand("ancestry set Alice human");
        alice.performCommand("ancestry choose");

        AncestryChoicesMenu page = choicesPage();
        assertEquals(Material.GRAY_STAINED_GLASS_PANE,
                page.getInventory().getItem(AncestryChoicesMenu.BACK_SLOT).getType(), "only the border");
        page.click(alice, AncestryChoicesMenu.BACK_SLOT, ClickType.LEFT);
        assertSame(page, choicesPage());
    }

    @Test
    void chooseWithNothingPendingSaysSo() {
        alice.performCommand("ancestry choose");
        staff.performCommand("ancestry set Alice dwarf");
        messages(alice);
        alice.performCommand("ancestry choose");

        assertEquals(List.of("[SkysongStats] Your character has no ancestry choices to make."), messages(alice));
    }

    @Test
    void chooseWithoutACharacterSaysSo() {
        ProfileManager.activeProfiles = new ArrayList<>();
        alice.performCommand("ancestry choose");

        assertEquals(List.of("[SkysongStats] You aren't playing a character."), messages(alice));
    }

    @Test
    void choosingAfterStaffChangedTheAncestryChangesNothing() {
        staff.performCommand("ancestry set Alice human");
        alice.performCommand("ancestry choose");
        AncestryChoicesMenu page = choicesPage();
        staff.performCommand("ancestry set Alice dwarf");
        messages(alice);

        page.click(alice, 10, ClickType.LEFT);
        page.click(alice, 12, ClickType.LEFT);
        page.click(alice, AncestryChoicesMenu.CONFIRM_SLOT, ClickType.LEFT);

        assertEquals("dwarf", ancestry().ancestry());
        assertEquals(0, value("str"));
        assertEquals(1, value("con"));
        assertEquals(List.of("[SkysongStats] Your ancestry changed while you were choosing. Run /ancestry choose again."),
                messages(alice));
    }

    @Test
    void rechooseTakesTheChoicesOffAndPrompts() {
        staff.performCommand("ancestry set Alice human");
        chooseStrAndCon();
        server.getScheduler().performOneTick();
        messages(alice);
        messages(staff);

        staff.performCommand("ancestry rechoose Alice");

        assertTrue(ancestry().choicesPending());
        assertEquals(0, value("str"));
        assertEquals(0, value("con"));
        assertEquals(List.of("[SkysongStats] Alice can make the Human choices of Default again."), messages(staff));
        server.getScheduler().performOneTick();
        assertEquals(List.of("[SkysongStats] Your ancestry, Human, has choices to make. [Choose]"), messages(alice));
    }

    @Test
    void rechooseKeepsTheFixedStats() {
        staff.performCommand("ancestry set Alice giant");
        alice.performCommand("ancestry choose");
        AncestryChoicesMenu page = choicesPage();
        page.click(alice, 10, ClickType.LEFT);
        page.click(alice, AncestryChoicesMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals(2, value("str"));

        staff.performCommand("ancestry rechoose Alice");

        assertEquals(1, value("str"));
    }

    @Test
    void rechooseWithoutChoicesSaysSo() {
        staff.performCommand("ancestry set Alice dwarf");
        messages(staff);
        staff.performCommand("ancestry rechoose Alice");

        assertEquals(List.of("[SkysongStats] Alice's character Default has no ancestry with choices to make."),
                messages(staff));
        assertFalse(ancestry().choicesPending());
    }

    @Test
    void removeTakesEverythingOffAndFreesTheField() {
        int spd = value("spd");
        staff.performCommand("ancestry set Alice dwarf");
        server.getScheduler().performOneTick();
        messages(staff);
        messages(alice);

        staff.performCommand("ancestry remove Alice");

        assertTrue(plugin.getAncestryService().of(character).isEmpty());
        assertEquals(0, value("con"));
        assertEquals(spd, value("spd"));
        assertEquals(CharacterProfile.NOT_SET, sheet.getAncestry());
        assertEquals(List.of("[SkysongStats] Alice's character Default no longer has an ancestry (Dwarf was taken off)."),
                messages(staff));
        assertEquals(List.of("[SkysongStats] Your character no longer has an ancestry."), messages(alice));
    }

    @Test
    void removeWithoutAnAncestrySaysSo() {
        staff.performCommand("ancestry remove Alice");

        assertEquals(List.of("[SkysongStats] Alice's character Default has no ancestry."), messages(staff));
    }

    @Test
    void staffCommandsWorkOnOfflinePlayers() {
        alice.disconnect();
        staff.performCommand("ancestry set Alice dwarf");

        assertEquals("dwarf", ancestry().ancestry());
        assertEquals(List.of("[SkysongStats] Alice's character Default is now Dwarf."), messages(staff));
    }

    @Test
    void anUnknownPlayerOrAncestryChangesNothing() {
        staff.performCommand("ancestry set Nobody dwarf");
        staff.performCommand("ancestry set Alice merfolk");

        assertEquals(List.of("[SkysongStats] Nobody has never played on this server.",
                "[SkysongStats] There is no ancestry called merfolk!"), messages(staff));
        assertTrue(plugin.getAncestryService().of(character).isEmpty());
    }

    @Test
    void aPlayerWithoutACharacterIsRefused() {
        ProfileManager.activeProfiles = new ArrayList<>();
        staff.performCommand("ancestry set Alice dwarf");

        assertEquals(List.of("[SkysongStats] Alice isn't playing a character with stats."), messages(staff));
    }

    @Test
    void playersCantUseTheStaffCommands() {
        alice.performCommand("ancestry set Alice dwarf");
        alice.performCommand("ancestry rechoose Alice");
        alice.performCommand("ancestry remove Alice");

        assertEquals(3, messages(alice).stream().filter(message -> message.contains("permission")).count());
        assertTrue(plugin.getAncestryService().of(character).isEmpty());
    }

    @Test
    void missingArgumentsShowTheUsage() {
        staff.performCommand("ancestry");
        staff.performCommand("ancestry set Alice");
        staff.performCommand("ancestry remove");

        assertEquals(List.of("[SkysongStats] Usage: /ancestry (choose/set/rechoose/remove)",
                "[SkysongStats] Usage: /ancestry set (player) (ancestry)",
                "[SkysongStats] Usage: /ancestry remove (player)"), messages(staff));
    }

    @Test
    void joiningOrSwitchingToACharacterWithPendingChoicesPrompts() {
        staff.performCommand("ancestry set Alice human");
        server.getScheduler().performOneTick();
        messages(alice);

        plugin.getAncestryPrompt().onJoin(new PlayerJoinEvent(alice, Component.empty()));
        plugin.getAncestryPrompt().onProfileSwitch(new ProfileUpdateEvent(alice, null,
                new CharacterId(alice.getUniqueId(), "Default")));

        assertEquals(List.of("[SkysongStats] Your ancestry, Human, has choices to make. [Choose]",
                "[SkysongStats] Your ancestry, Human, has choices to make. [Choose]"), messages(alice));
    }

    @Test
    void noPromptWithoutPendingChoicesOrForAnotherCharacter() {
        staff.performCommand("ancestry set Alice human");
        server.getScheduler().performOneTick();
        messages(alice);

        plugin.getAncestryPrompt().onProfileSwitch(new ProfileUpdateEvent(alice, null,
                new CharacterId(alice.getUniqueId(), "Other")));
        chooseStrAndCon();
        messages(alice);
        plugin.getAncestryPrompt().onJoin(new PlayerJoinEvent(alice, Component.empty()));

        assertEquals(List.of(), messages(alice));
    }

    @Test
    void everyoneMayChooseAndOnlyOpsMayManage() {
        Map<String, PermissionDefault> defaults = plugin.getPluginMeta().getPermissions().stream()
                .collect(Collectors.toMap(Permission::getName, Permission::getDefault));

        assertEquals(PermissionDefault.TRUE, defaults.get("skysongstats.ancestry.choose"));
        assertEquals(PermissionDefault.OP, defaults.get("skysongstats.ancestry.set"));
        assertEquals(PermissionDefault.OP, defaults.get("skysongstats.ancestry.rechoose"));
        assertEquals(PermissionDefault.OP, defaults.get("skysongstats.ancestry.remove"));
    }

    @Test
    void tabCompletionFollowsPermissions() {
        assertEquals(List.of("choose"), plugin.getCommand("ancestry").tabComplete(alice, "ancestry", new String[]{""}));
        assertEquals(List.of("choose", "set", "rechoose", "remove"),
                plugin.getCommand("ancestry").tabComplete(staff, "ancestry", new String[]{""}));
        assertEquals(List.of("dwarf", "dragonborn"),
                plugin.getCommand("ancestry").tabComplete(staff, "ancestry", new String[]{"set", "Alice", "d"}));
        assertEquals(List.of(), plugin.getCommand("ancestry").tabComplete(alice, "ancestry", new String[]{"set", ""}));
    }
}
