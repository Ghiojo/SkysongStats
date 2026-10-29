package org.skysongdev.skysongstats.messages;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Loads plugins/SkysongStats/messages.yml. The first start writes the bundled file
 * as it is. Later starts and reloads add every message the server's file lacks
 * (new messages from an update) with its default text and comments, and save the
 * file; texts the server owner changed are never touched.
 */
public final class MessagesFile {
    public static final String FILE_NAME = "messages.yml";

    private MessagesFile() {
    }

    /**
     * @param file        the server's messages.yml
     * @param bundledText the contents of the messages.yml inside the plugin jar
     * @param log         gets one line per added message, and any problem with the file
     */
    public static Messages load(File file, String bundledText, Consumer<String> log) {
        YamlConfiguration bundled = parse(bundledText);
        if (!file.exists()) {
            write(file, bundledText, log);
        }
        YamlConfiguration server = YamlConfiguration.loadConfiguration(file);
        List<String> added = addMissing(server, bundled);
        if (!added.isEmpty()) {
            save(server, file, log);
            added.forEach(key -> log.accept("messages.yml: added the new message '" + key + "'"));
        }
        return new Messages(server, bundled, log);
    }

    private static YamlConfiguration parse(String bundledText) {
        YamlConfiguration bundled = new YamlConfiguration();
        try {
            bundled.loadFromString(bundledText);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException("The plugin's own messages.yml is not valid YAML", e);
        }
        return bundled;
    }

    private static List<String> addMissing(YamlConfiguration server, YamlConfiguration bundled) {
        List<String> added = new ArrayList<>();
        for (String key : bundled.getKeys(true)) {
            if (server.contains(key)) {
                continue;
            }
            if (bundled.isConfigurationSection(key)) {
                server.createSection(key);
            } else {
                server.set(key, bundled.get(key));
                added.add(key);
            }
            server.setComments(key, bundled.getComments(key));
        }
        return added;
    }

    private static void write(File file, String text, Consumer<String> log) {
        try {
            Files.createDirectories(file.toPath().toAbsolutePath().getParent());
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.accept("messages.yml: could not create the file (" + e.getMessage() + "); using the default messages");
        }
    }

    private static void save(YamlConfiguration server, File file, Consumer<String> log) {
        try {
            server.save(file);
        } catch (IOException e) {
            log.accept("messages.yml: could not save the new messages (" + e.getMessage() + "); they use their default text");
        }
    }
}
