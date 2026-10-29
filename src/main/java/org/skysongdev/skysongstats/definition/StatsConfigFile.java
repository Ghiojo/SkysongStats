package org.skysongdev.skysongstats.definition;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Predicate;

// Reads plugins/SkysongStats/stats.yml. The first start writes the plugin's
// default file. A file with mistakes throws a DefinitionException listing all of
// them; on /ssreload the plugin then keeps the stats it already had.
public final class StatsConfigFile {
    public static final String FILE_NAME = "stats.yml";

    private StatsConfigFile() {
    }

    public static StatsConfig load(File file, String bundledText) {
        return load(file, bundledText, StatsConfigFile::isItem);
    }

    static StatsConfig load(File file, String bundledText, Predicate<String> isItemMaterial) {
        if (!file.exists()) {
            write(file, bundledText);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new DefinitionException(FILE_NAME, "could not be read: " + e.getMessage(), e);
        }
        return StatsConfigLoader.load(yaml, isItemMaterial);
    }

    private static boolean isItem(String name) {
        Material material = Material.matchMaterial(name);
        return material != null && material.isItem();
    }

    private static void write(File file, String text) {
        try {
            Files.createDirectories(file.toPath().toAbsolutePath().getParent());
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DefinitionException(FILE_NAME, "could not be created: " + e.getMessage(), e);
        }
    }
}
