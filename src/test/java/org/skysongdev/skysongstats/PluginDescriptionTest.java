package org.skysongdev.skysongstats;

import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks plugin.yml as the server parses it. Soft dependencies are matched by
 * the exact plugin name, so a misspelt name is silently ignored and the plugin
 * can load before the one it needs.
 */
class PluginDescriptionTest {

    private static PluginDescriptionFile load() throws Exception {
        try (InputStream in = PluginDescriptionTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml is not on the classpath");
            return new PluginDescriptionFile(in);
        }
    }

    @Test
    void softDependsOnLuckPermsByItsExactName() throws Exception {
        assertTrue(load().getSoftDepend().contains("LuckPerms"));
    }

    @Test
    void softDependsOnPlaceholderApiAndSkysongItems() throws Exception {
        PluginDescriptionFile description = load();
        assertTrue(description.getSoftDepend().contains("PlaceholderAPI"));
        assertTrue(description.getSoftDepend().contains("Skysong_Items"));
    }
}
