package org.skysongdev.skysongstats.items;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Counts the Skysong item tags that stats.yml lists under item-tags. Each item adds
// its tag's value times its stack size; a tag with requires-charges only counts on
// items whose charges are above 0. This is how Arcrot and Fictis were counted before;
// the values are now read through the SkysongItems API (see ItemTagReader).
public final class ItemTagCounter {

    private ItemTagCounter() {
    }

    // Every item tag any stat counts, each once.
    public static Set<ItemTagSource> sources(StatDefinitions definitions) {
        Set<ItemTagSource> sources = new LinkedHashSet<>();
        for (StatDefinition stat : definitions.all()) {
            sources.addAll(stat.itemTags());
        }
        return sources;
    }

    // The total of each source over the items, read through the reader.
    public static Map<ItemTagSource, Integer> count(ItemTagReader reader, ItemStack[] items,
                                                    Collection<ItemTagSource> sources) {
        List<ItemStack> inventory = Arrays.asList(items);
        Map<ItemTagSource, Integer> counts = new HashMap<>();
        for (ItemTagSource source : sources) {
            counts.put(source, reader.sum(inventory, source));
        }
        return counts;
    }
}
