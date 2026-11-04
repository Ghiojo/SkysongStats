package org.skysongdev.skysongstats.items;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongstats.definition.ItemTagSource;

import java.util.List;

// Reads the Skysong item tags that stats.yml lists under item-tags. SkysongItems owns
// the tags, so the plugin's reader asks its API (SkysongItemsTagReader); tests pass
// their own.
@FunctionalInterface
public interface ItemTagReader {

    // The tag's value on each item times the stack size, added up. With requiresCharges,
    // only items with charges left count. Empty slots and air count as 0.
    int sum(List<ItemStack> items, ItemTagSource source);
}
