package org.skysongdev.skysongstats;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.storage.CharacterStorage;
import org.skysongdev.skysongstats.storage.DatabaseSettings;
import org.skysongdev.skysongstats.storage.SkillRepository;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.UnsavedSkillRepository;
import org.skysongdev.skysongstats.storage.UnsavedStatRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// The plugin with its stats and skill XP kept in memory, for tests that have no
// MySQL. The older profile tables still fail and are logged, as they do without
// a database.
public class OfflineSkysongStats extends SkysongStats {

    // Loads it with the commands and permissions of the real plugin.yml.
    public static OfflineSkysongStats load() {
        return MockBukkit.loadWith(OfflineSkysongStats.class, description());
    }

    private static PluginDescriptionFile description() {
        try (InputStream in = Objects.requireNonNull(
                SkysongStats.class.getClassLoader().getResourceAsStream("plugin.yml"), "plugin.yml")) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            yaml.set("main", OfflineSkysongStats.class.getName());
            return new PluginDescriptionFile(new StringReader(yaml.saveToString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidConfigurationException | InvalidDescriptionException e) {
            throw new IllegalStateException("plugin.yml is broken", e);
        }
    }

    @Override
    protected CharacterStorage openDatabase(DatabaseSettings settings) {
        StatRepository stats = new UnsavedStatRepository(line -> { });
        SkillRepository skills = new UnsavedSkillRepository(line -> { });
        return new CharacterStorage() {
            @Override
            public StatRepository repository() {
                return stats;
            }

            @Override
            public SkillRepository skillRepository() {
                return skills;
            }

            @Override
            public void close() {
                // Nothing is queued: nothing is saved.
            }
        };
    }
}
