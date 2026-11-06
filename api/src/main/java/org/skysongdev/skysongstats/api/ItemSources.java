package org.skysongdev.skysongstats.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Item sources: containers other plugins keep for players (a backpack, a bag, a quiver)
 * whose items count toward the stats with {@code item-tags:} in stats.yml, exactly like
 * items in the player's inventory. With a source registered, an Arcrot item in a
 * plugin's backpack adds to Arcrot.
 *
 * <pre>{@code
 * ItemSourceRegistration backpacks = SkysongStatsApi.get().itemSources()
 *         .register(this, "backpack", player -> backpackOf(player).contents());
 * // Whenever a backpack's contents change:
 * SkysongStatsApi.get().itemSources().recount(player);
 * }</pre>
 *
 * <p>SkysongStats recounts on its own when a player's inventory changes, on join and on a
 * profile switch. It can't see a plugin's container change, so the plugin calls
 * {@link #recount(Player)} after one. A source is removed when its plugin disables, so a
 * plugin only registers it again in its {@code onEnable}.
 *
 * <p>A source that throws an exception or returns {@code null} counts nothing that time;
 * SkysongStats logs it once per registration and keeps counting the others.
 *
 * <p>Must be called on the server's main thread, and throws {@link IllegalStateException}
 * otherwise.
 *
 * <p>SkysongStats implements this interface; other plugins must not.
 */
@ApiStatus.NonExtendable
public interface ItemSources {

    /**
     * Adds an item source, and recounts online players with it.
     *
     * @param owner  the plugin the source belongs to; it must be enabled
     * @param id     a name for the source, unique within the plugin: lowercase letters,
     *               digits, {@code _} and {@code -}, for example {@code "backpack"}
     * @param source the items it holds for a player
     * @return the registration, to remove the source with
     * @throws IllegalArgumentException when the id is not valid, or the plugin already
     *                                  has a registered source with that id
     * @throws IllegalStateException    when the plugin is not enabled, or when called
     *                                  from another thread
     */
    @NotNull ItemSourceRegistration register(@NotNull Plugin owner, @NotNull String id, @NotNull ItemSource source);

    /**
     * Recounts a player's item tags now: their inventory and every source. Call it after
     * a source's items changed. Does nothing for a player who isn't playing a character.
     *
     * @throws IllegalStateException when called from another thread
     */
    void recount(@NotNull Player player);
}
