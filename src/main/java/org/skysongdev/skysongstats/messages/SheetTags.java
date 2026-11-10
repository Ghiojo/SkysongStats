package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extra {@code {tags}} that every sheet in messages.yml can use, filled in for
 * the player or character a sheet is about, such as {@code {injuries_hover_50}}. Messages asks
 * for them on every line it renders about a player; a tag the plugin passes itself
 * wins over one from here.
 */
public interface SheetTags {
    /** For tests and while nothing provides tags: no extra tags. */
    static SheetTags none() {
        return (player, line) -> Map.of();
    }

    /** Every tag of these, the first one winning when two know the same name. */
    static SheetTags all(SheetTags... sources) {
        List<SheetTags> list = List.of(sources);
        return new SheetTags() {
            @Override
            public Map<String, Component> tags(OfflinePlayer player, String line) {
                Map<String, Component> tags = new HashMap<>();
                list.forEach(source -> source.tags(player, line).forEach(tags::putIfAbsent));
                return tags;
            }

            @Override
            public Map<String, Component> tags(ProfileKey character, String line) {
                Map<String, Component> tags = new HashMap<>();
                list.forEach(source -> source.tags(character, line).forEach(tags::putIfAbsent));
                return tags;
            }
        };
    }

    /** The tags this line uses that this knows, by name, for the player. */
    Map<String, Component> tags(OfflinePlayer player, String line);

    /** The tags this line uses that this knows, by name, for a character, which may have no player. */
    default Map<String, Component> tags(ProfileKey character, String line) {
        return Map.of();
    }
}
