package org.skysongdev.skysongstats.importer;

import org.bukkit.configuration.ConfigurationSection;
import org.skysongdev.skysongstats.storage.DatabaseSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// What /ssimport chattweaks reads from SkysongChatTweaks' config.yml: its
// conditions, its injuries with the permission nodes that marked who knew each
// one, and the database those nodes were saved in. Values SkysongChatTweaks
// left out stay null, so the importer writes only what was there.
public record ChatTweaksConfig(List<Condition> conditions, List<Injury> injuries,
                               Optional<DatabaseSettings> database) {
    public static final String FILE_NAME = "config.yml";

    public record Condition(String id, String name, String description, String item) {
    }

    public record Injury(String id, String name, String description, String item, String severity,
                         List<String> permissions) {
    }

    public ChatTweaksConfig {
        conditions = List.copyOf(conditions);
        injuries = List.copyOf(injuries);
        Objects.requireNonNull(database, "database");
    }

    public static ChatTweaksConfig read(ConfigurationSection root) {
        Objects.requireNonNull(root, "root");
        ConfigurationSection database = root.getConfigurationSection("database");
        return new ChatTweaksConfig(conditions(root), injuries(root),
                Optional.ofNullable(database).map(DatabaseSettings::fromConfig));
    }

    private static List<Condition> conditions(ConfigurationSection root) {
        List<Condition> conditions = new ArrayList<>();
        for (ConfigurationSection condition : entries(root, "conditions")) {
            conditions.add(new Condition(condition.getName(), condition.getString("name"),
                    condition.getString("description"), condition.getString("item")));
        }
        return conditions;
    }

    private static List<Injury> injuries(ConfigurationSection root) {
        List<Injury> injuries = new ArrayList<>();
        for (ConfigurationSection injury : entries(root, "injuries")) {
            injuries.add(new Injury(injury.getName(), injury.getString("name"), injury.getString("description"),
                    injury.getString("item"), injury.getString("severity"), injury.getStringList("permissions")));
        }
        return injuries;
    }

    // The sections under root.<key>, skipping anything that isn't a section.
    private static List<ConfigurationSection> entries(ConfigurationSection root, String key) {
        ConfigurationSection section = root.getConfigurationSection(key);
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream()
                .map(section::getConfigurationSection)
                .filter(Objects::nonNull)
                .toList();
    }
}
