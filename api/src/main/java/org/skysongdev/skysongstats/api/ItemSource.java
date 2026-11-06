package org.skysongdev.skysongstats.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Items a plugin keeps for a player outside their inventory, such as a backpack, that count
 * toward the stats with {@code item-tags:} in stats.yml as if they were in the inventory.
 * Register one with {@link ItemSources#register}.
 *
 * <p>SkysongStats calls it on the server's main thread every time it recounts a player's
 * item tags: when the inventory changes, on join, on a profile switch, and when the plugin
 * asks for it with {@link ItemSources#recount(Player)}. Keep it fast and don't change
 * anything from it: return what the player holds now.
 *
 * <p>This is the one SkysongStats API interface other plugins implement.
 */
@FunctionalInterface
public interface ItemSource {

    /**
     * The items this source holds for the player now. SkysongStats only reads them.
     *
     * @param player an online player who is playing a character
     * @return the items; {@code null} entries and air count as nothing, and an empty
     *         collection when it holds nothing for this player
     */
    @NotNull Collection<? extends ItemStack> items(@NotNull Player player);
}
