package org.skysongdev.skysongstats.items;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongstats.definition.ItemTagSource;

import java.util.List;

// Reads the Skysong item tags that stats.yml lists under item-tags. SkysongItems owns
// the tags, so the real reader asks its API (SkysongItemsTagReader); NONE is used when
// SkysongItems is not installed.
@FunctionalInterface
public interface ItemTagReader {

    // Nothing to read without SkysongItems: every tag adds 0.
    ItemTagReader NONE = (items, source) -> 0;

    // The tag's value on each item times the stack size, added up. With requiresCharges,
    // only items with charges left count. Empty slots and air count as 0.
    int sum(List<ItemStack> items, ItemTagSource source);
}
