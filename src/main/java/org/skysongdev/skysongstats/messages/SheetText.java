package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * messages.yml lines rendered for the one a sheet is about: a player (other plugins'
 * {@code %placeholders%} and the sheet tags for their active character) or a character
 * with no player behind it (only the sheet tags for that character). Get one from
 * {@link Messages#about(org.bukkit.OfflinePlayer)} or
 * {@link Messages#about(org.skysongdev.skysongstats.storage.ProfileKey)}.
 */
public final class SheetText {
    private final Messages messages;
    private final UnaryOperator<String> external;
    private final Function<String, Map<String, Component>> tags;

    // external fills other plugins' %placeholders% into a line (or leaves it as it
    // is); tags gives the sheet tags a line uses.
    SheetText(Messages messages, UnaryOperator<String> external, Function<String, Map<String, Component>> tags) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.external = Objects.requireNonNull(external, "external");
        this.tags = Objects.requireNonNull(tags, "tags");
    }

    /** The key's text as one component; a list of lines is joined with line breaks. */
    public Component get(String key, Map<String, String> placeholders, Map<String, Component> components) {
        return render(String.join("\n", messages.rawLines(key)), placeholders, components);
    }

    /** The key's text as one component per line. */
    public List<Component> getLines(String key, Map<String, String> placeholders, Map<String, Component> components) {
        return renderLines(messages.rawLines(key), placeholders, components);
    }

    /** Lines that don't come from messages.yml, such as a layout another plugin gives, rendered the same way. */
    public List<Component> renderLines(List<String> lines, Map<String, String> placeholders,
                                       Map<String, Component> components) {
        return lines.stream().map(line -> render(line, placeholders, components)).toList();
    }

    // A component placeholder the caller passes wins over a sheet tag of the same name.
    private Component render(String text, Map<String, String> placeholders, Map<String, Component> components) {
        String filled = external.apply(text);
        Map<String, Component> withTags = new HashMap<>(tags.apply(filled));
        withTags.putAll(components);
        return messages.render(filled, placeholders, withTags);
    }
}
