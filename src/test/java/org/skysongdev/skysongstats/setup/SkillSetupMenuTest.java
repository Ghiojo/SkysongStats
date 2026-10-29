package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The /setup skill screen built from the default skills.yml. Texts are shown as
 * their key and placeholders, so the tests can read them.
 */
class SkillSetupMenuTest {
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
    private static final int FARMING = 11;
    private static final int TANDE = 24;
    private static final int MINING = 19;

    private ServerMock server;
    private PlayerMock player;
    private SkillsConfig config;
    private SkillSetupMenu menu;
    private final List<List<SkillDefinition>> confirmed = new ArrayList<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.getPluginManager().registerEvents(new SetupScreenListener(), MockBukkit.createMockPlugin());
        player = server.addPlayer("Alice");
        config = SetupTestData.defaultSkills();
        menu = new SkillSetupMenu(config.skills().setupSkills(), config.setup(), KEYS,
                (who, picked) -> confirmed.add(picked));
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

    private boolean glows(int slot) {
        var meta = item(slot).getItemMeta();
        return meta.hasEnchantmentGlintOverride() && meta.getEnchantmentGlintOverride();
    }

    private SkillDefinition skill(String id) {
        return config.skills().get(id).orElseThrow();
    }

    @Test
    void showsEverySkillInItsSlotWithItsIconAndLore() {
        assertEquals(Material.BONE, item(10).getType());
        assertEquals(Material.WHEAT, item(FARMING).getType());
        assertEquals(Material.COMPASS, item(TANDE).getType());
        assertEquals("skills.choice.skill-name{name=Transportation and Exploration}", name(TANDE));
        assertEquals("This skill has to do with your character's ability to explore, navigate and find their way.",
                plain(item(TANDE).getItemMeta().lore().getFirst()));
    }

    @Test
    void namesUseTheSetupColourOrElseTheSkillsColour() {
        assertEquals(TextColor.fromHexString("#f9f36d"), item(FARMING).getItemMeta().displayName().color());
        assertEquals(TextColor.fromHexString("#7fe95b"), item(TANDE).getItemMeta().displayName().color());
    }

    @Test
    void hasTheHeaderConfirmButtonAndBorder() {
        assertEquals(Material.NETHER_STAR, item(SkillSetupMenu.HEADER_SLOT).getType());
        assertEquals("skills.choice.title{points=2}", name(SkillSetupMenu.HEADER_SLOT));
        assertEquals("skills.choice.lore{points=2}",
                plain(item(SkillSetupMenu.HEADER_SLOT).getItemMeta().lore().getFirst()));
        assertEquals(Material.LIME_STAINED_GLASS_PANE, item(SkillSetupMenu.CONFIRM_SLOT).getType());
        assertEquals("skills.choice.confirm{}", name(SkillSetupMenu.CONFIRM_SLOT));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, item(0).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, item(53).getType());
        assertNull(item(9));
    }

    @Test
    void leftClickPicksAndGlowsAndRightClickUnpicks() {
        menu.click(player, FARMING, ClickType.LEFT);
        menu.click(player, TANDE, ClickType.LEFT);
        assertTrue(glows(FARMING));
        assertEquals("skills.choice.title{points=0}", name(SkillSetupMenu.HEADER_SLOT));
        menu.click(player, FARMING, ClickType.RIGHT);
        assertEquals(List.of(skill("tande")), menu.picks().picked());
        assertFalse(glows(FARMING));
        assertEquals("skills.choice.title{points=1}", name(SkillSetupMenu.HEADER_SLOT));
    }

    @Test
    void clickingAPickedSkillAgainKeepsItsGlow() {
        menu.click(player, FARMING, ClickType.LEFT);
        menu.click(player, FARMING, ClickType.LEFT);
        assertTrue(glows(FARMING));
        assertEquals(1, menu.picks().left());
    }

    @Test
    void noMoreThanTheAllowedPicks() {
        menu.click(player, FARMING, ClickType.LEFT);
        menu.click(player, TANDE, ClickType.LEFT);
        menu.click(player, MINING, ClickType.LEFT);
        assertFalse(glows(MINING));
        assertEquals(List.of(skill("farming"), skill("tande")), menu.picks().picked());
    }

    @Test
    void confirmingWithPicksLeftOnlyTellsThePlayerHowMany() {
        player.openInventory(menu.getInventory());
        menu.click(player, FARMING, ClickType.LEFT);
        menu.click(player, SkillSetupMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals("skills.choice.pick-more{picks=1}", plain(player.nextComponentMessage()));
        assertTrue(confirmed.isEmpty());
        assertEquals(menu, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void confirmingWithEveryPickUsedClosesAndPassesThePicks() {
        player.openInventory(menu.getInventory());
        menu.click(player, TANDE, ClickType.LEFT);
        menu.click(player, FARMING, ClickType.LEFT);
        menu.click(player, SkillSetupMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals(List.of(List.of(skill("tande"), skill("farming"))), confirmed);
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    @Test
    void closingClearsThePicksAndTheGlow() {
        player.openInventory(menu.getInventory());
        menu.click(player, FARMING, ClickType.LEFT);
        player.closeInventory();
        assertEquals(2, menu.picks().left());
        assertFalse(glows(FARMING));
    }

    @Test
    void clicksOnTheScreenAreHandledAndCancelled() {
        player.openInventory(menu.getInventory());
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                FARMING, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertEquals(List.of(skill("farming")), menu.picks().picked());
    }

    @Test
    void clicksInThePlayersOwnInventoryAreCancelledButIgnored() {
        player.openInventory(menu.getInventory());
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                SkillSetupMenu.SIZE + FARMING, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertEquals(2, menu.picks().left());
    }
}
