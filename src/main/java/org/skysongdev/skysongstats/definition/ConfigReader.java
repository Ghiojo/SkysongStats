package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

// Reads typed values out of a definition file (stats.yml, skills.yml). A value of
// the wrong type is added to errors with its path and the fallback is returned,
// so a loader can carry on and report every mistake in one go.
final class ConfigReader {
    private final List<String> errors;
    private final Predicate<String> isItemMaterial;

    // isItemMaterial tells whether a Material name exists and is an item. It is
    // passed in because Bukkit's Material lookup needs a running server.
    ConfigReader(List<String> errors, Predicate<String> isItemMaterial) {
        this.errors = errors;
        this.isItemMaterial = isItemMaterial;
    }

    Icon readIcon(ConfigurationSection section) {
        String where = section.getCurrentPath() + ".icon";
        String text = section.getString("icon");
        if (text == null) {
            errors.add(where + ": missing, use a Material (IRON_SWORD) or model:<namespace:id>");
            return Icon.parse("BARRIER");
        }
        try {
            Icon icon = Icon.parse(text);
            if (!icon.isModel() && !isItemMaterial.test(icon.material())) {
                errors.add(where + ": '" + text + "' is not an item Material");
            }
            return icon;
        } catch (IllegalArgumentException e) {
            errors.add(where + ": " + e.getMessage());
            return Icon.parse("BARRIER");
        }
    }

    boolean readBoolean(ConfigurationSection section, String key) {
        if (section.contains(key) && !section.isBoolean(key)) {
            errors.add(section.getCurrentPath() + "." + key + ": '" + section.get(key) + "' must be true or false");
            return false;
        }
        return section.getBoolean(key, false);
    }

    int readRequiredInt(ConfigurationSection section, String key) {
        if (!section.contains(key)) {
            errors.add(section.getCurrentPath() + "." + key + ": missing, must be a whole number");
            return 0;
        }
        return readInt(section, key, 0);
    }

    int readNonNegative(ConfigurationSection section, String key) {
        int value = readInt(section, key, 0);
        if (value < 0) {
            errors.add(section.getCurrentPath() + "." + key + ": can't be negative");
        }
        return value;
    }

    int readInt(ConfigurationSection section, String key, int fallback) {
        if (!section.contains(key)) {
            return fallback;
        }
        if (!section.isInt(key)) {
            errors.add(section.getCurrentPath() + "." + key + ": '" + section.get(key) + "' is not a whole number");
            return fallback;
        }
        return section.getInt(key);
    }

    static List<String> lowercase(List<String> names) {
        return names.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
    }
}
