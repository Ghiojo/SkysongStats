package org.skysongdev.skysongstats;

import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.skysongdev.skysongstats.storage.CharacterStorage;
import org.skysongdev.skysongstats.storage.DatabaseSettings;
import org.skysongdev.skysongstats.storage.ProficiencyRepository;
import org.skysongdev.skysongstats.storage.SkillRepository;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.UnsavedProficiencyRepository;
import org.skysongdev.skysongstats.storage.UnsavedSkillRepository;
import org.skysongdev.skysongstats.storage.UnsavedStatRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// The plugin with its stats, skill XP and proficiencies kept in memory, for tests that have no
// MySQL. The older profile tables still fail and are logged, as they do without
// a database.
public class OfflineSkysongStats extends SkysongStats {

    // Loads it with the commands and permissions of the real plugin.yml.
    public static OfflineSkysongStats load() {
        return MockBukkit.loadWith(OfflineSkysongStats.class, description());
    }

    // Only the main line changes. Going through YamlConfiguration would split the
    // dotted permission names into nested sections and lose them.
    private static PluginDescriptionFile description() {
        try (InputStream in = Objects.requireNonNull(
                SkysongStats.class.getClassLoader().getResourceAsStream("plugin.yml"), "plugin.yml")) {
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceFirst("(?m)^main: .*$", "main: " + OfflineSkysongStats.class.getName());
            return new PluginDescriptionFile(new StringReader(yaml));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidDescriptionException e) {
            throw new IllegalStateException("plugin.yml is broken", e);
        }
    }

    @Override
    protected CharacterStorage openDatabase(DatabaseSettings settings) {
        StatRepository stats = new UnsavedStatRepository(line -> { });
        SkillRepository skills = new UnsavedSkillRepository(line -> { });
        ProficiencyRepository proficiencies = new UnsavedProficiencyRepository(line -> { });
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
            public ProficiencyRepository proficiencyRepository() {
                return proficiencies;
            }

            @Override
            public void close() {
                // Nothing is queued: nothing is saved.
            }
        };
    }
}
