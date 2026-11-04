package org.skysongdev.skysongstats.items;

import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongitems.api.ItemTags;
import org.skysongdev.skysongitems.api.SkysongItemsApi;
import org.skysongdev.skysongstats.definition.ItemTagSource;

import java.util.List;
import java.util.Objects;

// Reads item tags through the SkysongItems API. A tag in stats.yml is a tag id from
// SkysongItems' config.yml (or one another plugin registered there); an id SkysongItems
// doesn't know, or a tag that isn't an int, adds 0.
//
// Only create this when SkysongItems is enabled: it is the one class that loads the
// SkysongItems API, so SkysongStats still starts without that plugin.
public final class SkysongItemsTagReader implements ItemTagReader {
    // The name in SkysongItems' plugin.yml, as plugin.yml's softdepend lists it.
    public static final String PLUGIN_NAME = "Skysong_Items";

    private final ItemTags tags;

    public SkysongItemsTagReader(ItemTags tags) {
        this.tags = Objects.requireNonNull(tags, "tags");
    }

    // The reader for the running SkysongItems. Call it once SkysongItems is enabled.
    public static SkysongItemsTagReader fromServer() {
        return new SkysongItemsTagReader(SkysongItemsApi.get().tags());
    }

    @Override
    public int sum(List<ItemStack> items, ItemTagSource source) {
        // The API reads on the main thread only; the inventory listener runs there.
        return tags.sumInt(items, source.tag(), source.requiresCharges());
    }
}
