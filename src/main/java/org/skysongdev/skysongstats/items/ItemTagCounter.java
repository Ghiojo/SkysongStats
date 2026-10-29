package org.skysongdev.skysongstats.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongstats.Utils.SkysongItemsKeys;
import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Counts the Skysong item tags that stats.yml lists under item-tags. Each item adds
// its tag's value times its stack size; a tag with requires-charges only counts on
// items whose "charges" tag is above 0. This is how Arcrot and Fictis were counted
// before, with the tags now read from the stat definitions.
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

    // The total of each source over the items. Empty slots and air count as 0.
    public static Map<ItemTagSource, Integer> count(ItemStack[] items, Collection<ItemTagSource> sources) {
        Map<ItemTagSource, Integer> counts = new HashMap<>();
        for (ItemTagSource source : sources) {
            counts.put(source, 0);
        }
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
                PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
                for (ItemTagSource source : sources) {
                    counts.merge(source, value(data, source) * item.getAmount(), Integer::sum);
                }
            }
        }
        return counts;
    }

    private static int value(PersistentDataContainer data, ItemTagSource source) {
        if (source.requiresCharges() && !hasCharges(data)) {
            return 0;
        }
        NamespacedKey key = new NamespacedKey(SkysongItemsKeys.NAMESPACE, source.tag());
        Integer value = data.get(key, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    private static boolean hasCharges(PersistentDataContainer data) {
        Integer charges = data.get(SkysongItemsKeys.CHARGES, PersistentDataType.INTEGER);
        return charges != null && charges > 0;
    }
}
