package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The texts of messages.yml, ready to send. Each key holds one line or a list of
 * lines in MiniMessage. {@code {prefix}} is replaced by the file's prefix, any other
 * {@code {name}} by the value passed for it. Values are escaped, so text a player
 * typed (a name, a profile name) is shown as written and cannot add formatting.
 * <p>
 * A key the server's file lacks, or holds as a section, falls back to the bundled
 * text and is reported once through {@code warn}. Reloading builds a new Messages.
 */
public final class Messages {
    public static final String PREFIX_KEY = "prefix";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z0-9_-]+)}");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ConfigurationSection texts;
    private final ConfigurationSection defaults;
    private final Consumer<String> warn;
    private final Set<String> warnedKeys = new HashSet<>();

    public Messages(ConfigurationSection texts, ConfigurationSection defaults, Consumer<String> warn) {
        this.texts = Objects.requireNonNull(texts, "texts");
        this.defaults = Objects.requireNonNull(defaults, "defaults");
        this.warn = Objects.requireNonNull(warn, "warn");
    }

    /** Whether the server's file or the bundled file has text for this key. */
    public boolean has(String key) {
        return readLines(texts, key).or(() -> readLines(defaults, key)).isPresent();
    }

    public Component get(String key) {
        return get(key, Map.of());
    }

    /** The message as one component; a list of lines is joined with line breaks. */
    public Component get(String key, Map<String, String> placeholders) {
        return render(String.join("\n", lines(key)), placeholders);
    }

    public List<Component> getLines(String key) {
        return getLines(key, Map.of());
    }

    /** The message as one component per line; a single line gives a list of one. */
    public List<Component> getLines(String key, Map<String, String> placeholders) {
        return lines(key).stream().map(line -> render(line, placeholders)).toList();
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    /** Sends each line of the message as its own chat line. */
    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        getLines(key, placeholders).forEach(sender::sendMessage);
    }

    private List<String> lines(String key) {
        return readLines(texts, key).orElseGet(() -> defaultLines(key));
    }

    private List<String> defaultLines(String key) {
        Optional<List<String>> lines = readLines(defaults, key);
        if (lines.isPresent()) {
            warnOnce(key, "messages.yml: '" + key + "' is missing or not text, using the default message");
        } else {
            warnOnce(key, "messages.yml: no message '" + key + "', not even in the plugin's default file");
        }
        return lines.orElse(List.of(key));
    }

    private static Optional<List<String>> readLines(ConfigurationSection section, String key) {
        if (section.isList(key)) {
            return Optional.of(section.getStringList(key));
        }
        return Optional.ofNullable(section.isString(key) ? section.getString(key) : null).map(List::of);
    }

    private void warnOnce(String key, String warning) {
        if (warnedKeys.add(key)) {
            warn.accept(warning);
        }
    }

    private Component render(String text, Map<String, String> placeholders) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(valueOf(matcher.group(1), placeholders, matcher.group())));
        }
        matcher.appendTail(result);
        return MINI_MESSAGE.deserialize(result.toString());
    }

    private String valueOf(String name, Map<String, String> placeholders, String unknown) {
        if (placeholders.containsKey(name)) {
            return MINI_MESSAGE.escapeTags(placeholders.get(name));
        }
        if (name.equals(PREFIX_KEY)) {
            return String.join("", lines(PREFIX_KEY));
        }
        return unknown;
    }
}
