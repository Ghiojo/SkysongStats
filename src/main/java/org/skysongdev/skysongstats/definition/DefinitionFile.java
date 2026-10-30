package org.skysongdev.skysongstats.definition;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

// Opens a definition file in the plugin folder (stats.yml, skills.yml). The first
// start writes the plugin's default file. A file that can't be written or read
// throws a DefinitionException naming it.
final class DefinitionFile {
    private DefinitionFile() {
    }

    static YamlConfiguration read(File file, String bundledText, String fileName) {
        if (!file.exists()) {
            write(file, bundledText, fileName);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new DefinitionException(fileName, "could not be read: " + e.getMessage(), e);
        }
        return yaml;
    }

    static boolean isItem(String name) {
        Material material = Material.matchMaterial(name);
        return material != null && material.isItem();
    }

    private static void write(File file, String text, String fileName) {
        try {
            Files.createDirectories(file.toPath().toAbsolutePath().getParent());
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DefinitionException(fileName, "could not be created: " + e.getMessage(), e);
        }
    }
}
