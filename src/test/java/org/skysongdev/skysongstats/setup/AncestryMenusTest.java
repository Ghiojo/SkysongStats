package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.character.AncestryPicks;
import org.skysongdev.skysongstats.definition.AncestriesConfigLoader;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryDefinitions;
import org.skysongdev.skysongstats.definition.StatDefinitions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The /setup ancestry list and the ancestry choices page, built from the default
 * stats.yml and ancestries.yml. Texts are shown as their key and placeholders,
 * so the tests can read them.
 */
class AncestryMenusTest {
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
    private static final StatDefinitions STATS = SetupTestData.defaultConfig().stats();
    private static final AncestryDefinitions ANCESTRIES = SetupTestData.defaultAncestries();

    private ServerMock server;
    private PlayerMock player;
    private final List<AncestryDefinition> picked = new ArrayList<>();
    private final List<AncestryPicks> confirmed = new ArrayList<>();
    private int backs;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.getPluginManager().registerEvents(new SetupScreenListener(), MockBukkit.createMockPlugin());
        player = server.addPlayer("Alice");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    private static String name(Inventory inventory, int slot) {
        return plain(inventory.getItem(slot).getItemMeta().displayName());
    }

    private static List<String> lore(Inventory inventory, int slot) {
        return inventory.getItem(slot).getItemMeta().lore().stream().map(AncestryMenusTest::plain).toList();
    }

    private static boolean glows(Inventory inventory, int slot) {
        var meta = inventory.getItem(slot).getItemMeta();
        return meta.hasEnchantmentGlintOverride() && meta.getEnchantmentGlintOverride();
    }

    private static AncestryDefinition ancestry(String id) {
        return ANCESTRIES.get(id).orElseThrow();
    }

    private AncestryChoicesMenu choices(String id, boolean withBack) {
        return new AncestryChoicesMenu(ancestry(id), STATS, KEYS,
                withBack ? Optional.of(who -> backs++) : Optional.empty(), (who, picks) -> confirmed.add(picks));
    }

    // Row r (from 1) holds choice r; its first slot explains it.
    private static int slot(int choice, int item) {
        return (choice + 1) * 9 + 1 + item;
    }

    @Test
    void theListShowsEachAncestryInItsSlotWithWhatItGives() {
        AncestrySetupMenu menu = new AncestrySetupMenu(ANCESTRIES.all(), STATS, KEYS, (who, ancestry) -> picked.add(ancestry));
        Inventory inventory = menu.getInventory();
        assertEquals(Material.IRON_PICKAXE, inventory.getItem(10).getType());
        assertEquals("setup.ancestry.item-name{name=Dwarf}", name(inventory, 10));
        assertEquals(List.of("setup.ancestry.adjustment{amount=+1, stat=Constitution}",
                "setup.ancestry.adjustment{amount=-1, stat=Speed}",
                "Stout folk of the deep halls.",
                "setup.ancestry.click{}"), lore(inventory, 10));
        assertEquals("setup.ancestry.header.name{}", name(inventory, AncestrySetupMenu.HEADER_SLOT));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(0).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(53).getType());
    }

    @Test
    void theListDescribesEveryKindOfChoice() {
        Inventory inventory = new AncestrySetupMenu(ANCESTRIES.all(), STATS, KEYS, (who, ancestry) -> { }).getInventory();
        assertEquals("setup.ancestry.choice-stats{amount=+1, name=Grace, picks=1, "
                + "stats=Dexterityseparator{}Focus}".replace("separator{}", "setup.ancestry.separator{}"),
                lore(inventory, 11).getFirst());
        assertEquals("setup.ancestry.choice-any{amount=+1, name=Versatile, picks=2, stats=Strength",
                lore(inventory, 12).getFirst().substring(0, "setup.ancestry.choice-any{amount=+1, name=Versatile, picks=2, stats=Strength".length()));
        assertTrue(lore(inventory, 14).getFirst().startsWith(
                "setup.ancestry.choice-package{name=Upbringing, options=Brawler (+2 Strength)"), lore(inventory, 14).toString());
    }

    @Test
    void clickingAnAncestryPicksIt() {
        AncestrySetupMenu menu = new AncestrySetupMenu(ANCESTRIES.all(), STATS, KEYS, (who, ancestry) -> picked.add(ancestry));
        menu.open(player);
        menu.click(player, 0, ClickType.LEFT);
        menu.click(player, 13, ClickType.LEFT);
        assertEquals(List.of(ancestry("goblin")), picked);
        menu.closed();
    }

    @Test
    void eachChoiceHasARowOfWhatItCanPick() {
        Inventory inventory = choices("human", true).getInventory();
        assertEquals("setup.ancestry-choices.choice-stats.name{amount=+1, left=2, name=Versatile, picks=2}",
                name(inventory, 9));
        // from: any is every setup stat, in stats.yml order.
        assertEquals(Material.IRON_SWORD, inventory.getItem(slot(0, 0)).getType());
        assertEquals("setup.ancestry-choices.stat.name{amount=+1, stat=Strength}", name(inventory, slot(0, 0)));
        assertEquals(List.of("setup.ancestry-choices.open{}"), lore(inventory, slot(0, 0)));
        assertNull(inventory.getItem(slot(0, 5)));
        assertEquals("setup.ancestry-choices.back{}", name(inventory, AncestryChoicesMenu.BACK_SLOT));
        assertEquals("setup.ancestry-choices.confirm{}", name(inventory, AncestryChoicesMenu.CONFIRM_SLOT));
        assertEquals("setup.ancestry-choices.header.name{ancestry=Human}", name(inventory, AncestryChoicesMenu.HEADER_SLOT));
    }

    @Test
    void clickingPicksAndUnpicksStats() {
        AncestryChoicesMenu menu = choices("human", false);
        menu.click(player, slot(0, 0), ClickType.LEFT);
        menu.click(player, slot(0, 2), ClickType.LEFT);
        // Both picks used: a third doesn't fit.
        menu.click(player, slot(0, 1), ClickType.LEFT);
        assertEquals(AncestryPicks.none().withStats(0, List.of("str", "con")), menu.picks());
        assertTrue(glows(menu.getInventory(), slot(0, 0)));
        assertEquals(Material.LIME_DYE, menu.getInventory().getItem(9).getType());

        menu.click(player, slot(0, 0), ClickType.LEFT);
        assertEquals(AncestryPicks.none().withStats(0, List.of("con")), menu.picks());
        assertFalse(glows(menu.getInventory(), slot(0, 0)));
    }

    @Test
    void aSinglePickIsReplacedByTheNextClick() {
        AncestryChoicesMenu menu = choices("elf", false);
        menu.click(player, slot(0, 0), ClickType.LEFT);
        menu.click(player, slot(0, 1), ClickType.LEFT);
        assertEquals(AncestryPicks.none().withStats(0, List.of("foc")), menu.picks());
    }

    @Test
    void statsTheAncestryAlreadyRaisesAreBlocked() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("ancestries:\n  kobold: {icon: STONE, slot: 10, stats: {dex: 1},"
                + " choices: [{amount: 1, from: [str, dex]}]}\n");
        AncestryDefinition kobold = AncestriesConfigLoader.load(yaml, STATS, name -> true).get("kobold").orElseThrow();
        AncestryChoicesMenu menu = new AncestryChoicesMenu(kobold, STATS, KEYS, Optional.empty(), (who, picks) -> { });
        assertEquals(List.of("setup.ancestry-choices.blocked{}"), lore(menu.getInventory(), slot(0, 1)));
        menu.click(player, slot(0, 1), ClickType.LEFT);
        assertEquals(AncestryPicks.none(), menu.picks());
    }

    @Test
    void aStackingChoiceMayPickABoostedStat() {
        AncestryChoicesMenu giant = choices("giant", false);
        assertEquals(List.of("setup.ancestry-choices.open{}"), lore(giant.getInventory(), slot(0, 0)));
        giant.click(player, slot(0, 0), ClickType.LEFT);
        assertEquals(AncestryPicks.none().withStats(0, List.of("str")), giant.picks());
    }

    @Test
    void packagesArePickedOneAtATime() {
        AncestryChoicesMenu orc = choices("orc", false);
        Inventory inventory = orc.getInventory();
        assertEquals("setup.ancestry-choices.choice-package.name{name=Upbringing}", name(inventory, 9));
        assertEquals("setup.ancestry-choices.package.name{adjustments=+2 Strength, name=Brawler}", name(inventory, slot(0, 0)));
        assertEquals(List.of("setup.ancestry-choices.package.lore{adjustments=+2 Strength, name=Brawler}",
                "setup.ancestry-choices.open{}"), lore(inventory, slot(0, 0)));
        orc.click(player, slot(0, 0), ClickType.LEFT);
        orc.click(player, slot(0, 1), ClickType.LEFT);
        assertEquals(AncestryPicks.none().withPackage(0, 1), orc.picks());
        orc.click(player, slot(0, 1), ClickType.LEFT);
        assertEquals(AncestryPicks.none(), orc.picks());
    }

    @Test
    void confirmingNeedsEveryChoice() {
        AncestryChoicesMenu menu = choices("human", false);
        menu.click(player, slot(0, 0), ClickType.LEFT);
        menu.click(player, AncestryChoicesMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals("setup.ancestry-choices.incomplete{choice=Versatile}", plain(player.nextComponentMessage()));
        assertTrue(confirmed.isEmpty());

        menu.click(player, slot(0, 1), ClickType.LEFT);
        menu.click(player, AncestryChoicesMenu.CONFIRM_SLOT, ClickType.LEFT);
        assertEquals(List.of(AncestryPicks.none().withStats(0, List.of("str", "dex"))), confirmed);
    }

    @Test
    void backReturnsOnlyWhenOffered() {
        AncestryChoicesMenu withBack = choices("elf", true);
        withBack.click(player, AncestryChoicesMenu.BACK_SLOT, ClickType.LEFT);
        assertEquals(1, backs);

        AncestryChoicesMenu withoutBack = choices("elf", false);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, withoutBack.getInventory().getItem(AncestryChoicesMenu.BACK_SLOT).getType());
        withoutBack.click(player, AncestryChoicesMenu.BACK_SLOT, ClickType.LEFT);
        assertEquals(1, backs);
    }

    @Test
    void closingDropsThePicks() {
        AncestryChoicesMenu menu = choices("elf", false);
        menu.click(player, slot(0, 0), ClickType.LEFT);
        menu.closed();
        assertEquals(AncestryPicks.none(), menu.picks());
        assertFalse(glows(menu.getInventory(), slot(0, 0)));
    }

    @Test
    void clicksOutsideTheRowsDoNothing() {
        AncestryChoicesMenu menu = choices("elf", false);
        menu.click(player, 9, ClickType.LEFT);
        menu.click(player, slot(0, 5), ClickType.LEFT);
        menu.click(player, slot(3, 0), ClickType.LEFT);
        assertEquals(AncestryPicks.none(), menu.picks());
    }
}
