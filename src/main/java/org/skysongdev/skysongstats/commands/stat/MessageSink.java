package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

// Where the stat commands send their text: messages.yml keys with {name}
// placeholders. The plugin passes its messages.yml loader; tests record the keys.
public interface MessageSink {
    void send(CommandSender to, String key, Map<String, String> placeholders);

    // A message about one player, such as their stat sheet: other plugins'
    // %placeholders% in it are filled in for that player. A sink without
    // PlaceholderAPI sends it like any other message.
    default void send(CommandSender to, String key, Map<String, String> placeholders, OfflinePlayer about) {
        send(to, key, placeholders);
    }

    // Whether messages.yml has this key, so a stat can fall back to the default
    // messages when it has none of its own.
    boolean has(String key);

    // The lines of a key as written in messages.yml, placeholders not filled in,
    // so the stat sheet can see which stats its lines show.
    List<String> lines(String key);
}
