package org.skysongdev.skysongstats.items;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Counting the item tags stats.yml lists. How one tag is added up over the items
 * (value times stack size, charges) is SkysongItems' job, tested through
 * SkysongItemsTagReaderTest; the counter asks the reader once per tag.
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

    @Test
    void everySourceGetsTheReadersTotal() {
        Map<ItemTagSource, Integer> totals = Map.of(ARCROT, 11, CHARGED_ARCROT, 4, FICTIS, 6);
        ItemTagReader reader = (items, source) -> totals.get(source);

        assertEquals(totals, ItemTagCounter.count(reader, List.of(), DEFAULT_SOURCES));
    }

    @Test
    void theReaderSeesTheWholeInventoryInOrder() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack stone = new ItemStack(Material.STONE, 64);
        List<ItemStack> inventory = Arrays.asList(sword, null, stone);
        List<List<ItemStack>> seen = new ArrayList<>();

        ItemTagCounter.count((items, source) -> {
            seen.add(items);
            return 0;
        }, inventory, List.of(ARCROT));

        assertEquals(List.of(Arrays.asList(sword, null, stone)), seen);
    }

    @Test
    void noSourcesCountNothing() {
        assertEquals(Map.of(), ItemTagCounter.count((items, source) -> 1, List.of(), List.of()));
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
