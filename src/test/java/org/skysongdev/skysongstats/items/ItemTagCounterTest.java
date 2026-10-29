package org.skysongdev.skysongstats.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.Utils.SkysongItemsKeys;
import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Counting Skysong item tags. The cases match how Arcrot and Fictis were counted
 * before (AfflictionsListenerTest), with the tags given as ItemTagSources.
 */
class ItemTagCounterTest {
    private static final ItemTagSource ARCROT = new ItemTagSource("arcrot", false);
    private static final ItemTagSource CHARGED_ARCROT = new ItemTagSource("charged_arcrot", true);
    private static final ItemTagSource FICTIS = new ItemTagSource("fictis", false);
    private static final List<ItemTagSource> DEFAULT_SOURCES = List.of(ARCROT, CHARGED_ARCROT, FICTIS);

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack item(int amount, Object... keysAndValues) {
        ItemStack item = new ItemStack(Material.STICK, amount);
        ItemMeta meta = item.getItemMeta();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            meta.getPersistentDataContainer().set((NamespacedKey) keysAndValues[i], PersistentDataType.INTEGER, (Integer) keysAndValues[i + 1]);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Map<ItemTagSource, Integer> count(ItemStack... items) {
        return ItemTagCounter.count(items, DEFAULT_SOURCES);
    }

    @Test
    void emptySlotsAirAndUntaggedItemsCountAsZero() {
        assertEquals(Map.of(ARCROT, 0, CHARGED_ARCROT, 0, FICTIS, 0),
                count(null, new ItemStack(Material.AIR), new ItemStack(Material.STONE, 64)));
    }

    @Test
    void valuesAreMultipliedByStackSizeAndSummed() {
        Map<ItemTagSource, Integer> counts = count(
                item(3, SkysongItemsKeys.ARCROT, 2),
                item(1, SkysongItemsKeys.ARCROT, 5),
                item(2, SkysongItemsKeys.FICTIS, 3));
        assertEquals(11, counts.get(ARCROT));
        assertEquals(6, counts.get(FICTIS));
    }

    @Test
    void chargedTagsCountOnlyWhileChargesRemain() {
        assertEquals(8, count(item(2, SkysongItemsKeys.CHARGED_ARCROT, 4, SkysongItemsKeys.CHARGES, 1)).get(CHARGED_ARCROT));
        assertEquals(0, count(item(1, SkysongItemsKeys.CHARGED_ARCROT, 4, SkysongItemsKeys.CHARGES, 0)).get(CHARGED_ARCROT));
        assertEquals(0, count(item(1, SkysongItemsKeys.CHARGED_ARCROT, 4)).get(CHARGED_ARCROT), "no charges tag");
    }

    @Test
    void oneItemCanCarrySeveralTags() {
        Map<ItemTagSource, Integer> counts = count(
                item(1, SkysongItemsKeys.ARCROT, 3, SkysongItemsKeys.CHARGED_ARCROT, 4, SkysongItemsKeys.CHARGES, 2));
        assertEquals(3, counts.get(ARCROT));
        assertEquals(4, counts.get(CHARGED_ARCROT));
    }

    @Test
    void keysFromAnotherNamespaceAreIgnored() {
        assertEquals(0, count(item(1, new NamespacedKey("other_plugin", "arcrot"), 9)).get(ARCROT));
    }

    @Test
    void anyTagNamedInStatsYmlIsCounted() {
        ItemTagSource sanity = new ItemTagSource("sanity_drain", false);
        ItemStack charm = item(2, new NamespacedKey(SkysongItemsKeys.NAMESPACE, "sanity_drain"), 5);
        assertEquals(Map.of(sanity, 10), ItemTagCounter.count(new ItemStack[]{charm}, List.of(sanity)));
    }

    @Test
    void sourcesListsEveryStatsTagsOnce() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                stats:
                  a: {type: bar, max: 10, item-tags: [{tag: arcrot}, {tag: fictis}]}
                  b: {type: bar, max: 10, item-tags: [{tag: fictis}, {tag: charged_arcrot, requires-charges: true}]}
                  c: {}
                """);
        assertEquals(Set.of(ARCROT, FICTIS, CHARGED_ARCROT),
                ItemTagCounter.sources(StatsConfigLoader.load(config, name -> true).stats()));
    }
}
