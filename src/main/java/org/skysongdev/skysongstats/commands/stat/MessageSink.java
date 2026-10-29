package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;

import java.util.Map;

// Where the stat commands send their text: messages.yml keys with {name}
// placeholders. The plugin passes its messages.yml loader; tests record the keys.
public interface MessageSink {
    void send(CommandSender to, String key, Map<String, String> placeholders);

    // Whether messages.yml has this key, so a stat can fall back to the default
    // messages when it has none of its own.
    boolean has(String key);
}
