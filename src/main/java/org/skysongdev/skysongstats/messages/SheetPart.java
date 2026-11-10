package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.Objects;

/**
 * One messages.yml key of a sheet with what fills it in, so the same sheet can be sent
 * to a command's sender or rendered for another plugin.
 *
 * @param key          the messages.yml key
 * @param placeholders text placeholders, escaped when filled in
 * @param components   component placeholders, such as player-styled text
 */
public record SheetPart(String key, Map<String, String> placeholders, Map<String, Component> components) {

    public SheetPart {
        Objects.requireNonNull(key, "key");
        placeholders = Map.copyOf(placeholders);
        components = Map.copyOf(components);
    }

    /** A part with text placeholders only. */
    public SheetPart(String key, Map<String, String> placeholders) {
        this(key, placeholders, Map.of());
    }
}
