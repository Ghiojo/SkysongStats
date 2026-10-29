package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.definition.Icon;
import org.skysongdev.skysongstats.definition.SetupRules;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDisplay;
import org.skysongdev.skysongstats.definition.StatSetup;
import org.skysongdev.skysongstats.definition.StatsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The /setup screen built from the default stats.yml. Texts are shown as their
 * key and placeholders, so the tests can read them.
 */
class StatSetupMenuTest {
    private static final MenuTexts KEYS = new MenuTexts() {
        @Override
        public Component line(String key, Map<String, String> placeholders) {
            return Component.text(key + new TreeMap<>(placeholders));
        }

        @Override
        public List<Component> lines(String key, Map<String, String> placeholders) {
            return List.of(line(key, placeholders));
        }
    };

    private ServerMock server;
    private PlayerMock player;
    private StatSetupMenu menu;
    private final List<Map<String, Integer>> confirmed = new ArrayList<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.getPluginManager().registerEvents(new StatSetupListener(), MockBukkit.createMockPlugin());
        player = server.addPlayer("Alice");
        StatsConfig config = SetupTestData.defaultConfig();
        menu = new StatSetupMenu(config.stats().setupStats(), config.setupRules(), KEYS,
                (who, values) -> confirmed.add(values));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    private ItemStack item(int slot) {
        return menu.getInventory().getItem(slot);
    }

    private String name(int slot) {
        return plain(item(slot).getItemMeta().displayName());
    }

    private void click(int slot, ClickType click, int times) {
        for (int i = 0; i < times; i++) {
            menu.click(player, slot, click);
        }
    }

    @Test
    void showsEachSetupStatInItsSlotWithItsIcon() {
        assertEquals(Material.IRON_SWORD, item(9).getType());
        assertEquals(Material.BOW, item(11).getType());
        assertEquals(Material.APPLE, item(13).getType());
        assertEquals(Material.ENDER_EYE, item(15).getType());
        assertEquals(Material.FEATHER, item(17).getType());
        assertEquals("setup.stat.name{max=10, min=3, name=Speed, value=6}", name(17));
    }

    @Test
    void statLoreIsTheMessagesLinesThenTheStatsYmlLore() {
        List<Component> lore = item(9).getItemMeta().lore();
        assertEquals("setup.stat.lore{max=4, min=-3, name=Strength, value=0}", plain(lore.get(0)));
        assertEquals("Determines your character's Physical Strength", plain(lore.get(1)));
    }

    @Test
    void theStatNameUsesTheStatsColour() {
        assertEquals(NamedTextColor.RED, item(9).getItemMeta().displayName().color());
    }

    @Test
    void hasTheHeaderConfirmButtonAndBorder() {
        assertEquals(Material.NETHER_STAR, item(StatSetup.HEADER_SLOT).getType());
        assertEquals("setup.header.name{negative-points=3, points=7}", name(StatSetup.HEADER_SLOT));
        assertEquals(Material.LIME_STAINED_GLASS_PANE, item(StatSetup.CONFIRM_SLOT).getType());
        assertEquals("setup.confirm.name{}", name(StatSetup.CONFIRM_SLOT));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, item(0).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, item(26).getType());
        assertNull(item(10));
    }

    @Test
    void leftClickRaisesAndRightClickLowers() {
        click(9, ClickType.LEFT, 2);
        click(11, ClickType.RIGHT, 1);
        click(13, ClickType.MIDDLE, 1);
        assertEquals(Map.of("str", 2, "dex", -1, "con", 0, "foc", 0, "spd", 6), menu.allocation().values());
        assertTrue(name(9).contains("value=2"), name(9));
        assertEquals("setup.header.name{negative-points=2, points=6}", name(StatSetup.HEADER_SLOT));
    }

    @Test
    void confirmingWithPointsLeftOnlyTellsThePlayer() {
        player.openInventory(menu.getInventory());
        menu.click(player, StatSetup.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals("setup.points-left{}", plain(player.nextComponentMessage()));
        assertTrue(confirmed.isEmpty());
        assertEquals(menu, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void confirmingWithEveryPointSpentClosesAndPassesTheValues() {
        player.openInventory(menu.getInventory());
        click(9, ClickType.LEFT, 4);
        click(17, ClickType.LEFT, 3);
        menu.click(player, StatSetup.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals(List.of(Map.of("str", 4, "dex", 0, "con", 0, "foc", 0, "spd", 9)), confirmed);
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    @Test
    void closingStartsOver() {
        player.openInventory(menu.getInventory());
        click(9, ClickType.LEFT, 2);
        player.closeInventory();
        assertEquals(7, menu.allocation().points());
        assertTrue(name(9).contains("value=0"), name(9));
    }

    @Test
    void clicksInTheMenuAreHandledAndCancelled() {
        player.openInventory(menu.getInventory());
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                9, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertEquals(1, menu.allocation().values().get("str"));
    }

    // MockBukkit 4.116 drops the item model when it copies item meta, so only the
    // paper item can be checked here.
    @Test
    void clicksInThePlayersOwnInventoryAreCancelledButIgnored() {
        player.openInventory(menu.getInventory());
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                StatSetupMenu.SIZE + 9, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertEquals(7, menu.allocation().points());
    }

    @Test
    void anItemModelIconIsShownOnPaper() {
        StatDefinition luck = new StatDefinition("luck", new StatDisplay("Luck", "", List.of()), 0, null, null,
                List.of(), Map.of(), new StatSetup(Icon.parse("model:skysong:clover"), 10, 0, 1, List.of()),
                PermissionDefault.OP);
        StatSetupMenu modelMenu = new StatSetupMenu(List.of(luck), new SetupRules(1, 0), KEYS, (who, values) -> { });
        ItemStack icon = modelMenu.getInventory().getItem(10);
        assertEquals(Material.PAPER, icon.getType());
    }
}
