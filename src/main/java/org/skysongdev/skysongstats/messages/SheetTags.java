package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;

import java.util.Map;

/**
 * Extra {@code {tags}} that every sheet in messages.yml can use, filled in for
 * the player a sheet is about, such as {@code {injuries_hover_50}}. Messages asks
 * for them on every line it renders about a player; a tag the plugin passes itself
 * wins over one from here.
 */
public interface SheetTags {
    /** For tests and while nothing provides tags: no extra tags. */
    SheetTags NONE = (player, line) -> Map.of();

    /** The tags this line uses that this knows, by name, for the player. */
    Map<String, Component> tags(OfflinePlayer player, String line);
}
