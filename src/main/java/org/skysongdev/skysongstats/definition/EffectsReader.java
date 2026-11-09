package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// Opens the optional "effects:" section of an injury or condition and checks it
// only holds the keys that kind of entry may have, so a typo ("modifier:") is an
// error instead of an effect that silently does nothing.
final class EffectsReader {
    static final String KEY = "effects";

    private final List<String> errors;

    EffectsReader(List<String> errors) {
        this.errors = errors;
    }

    // Empty when the entry has no effects; an effects: that isn't a section is an error.
    Optional<ConfigurationSection> open(ConfigurationSection entry, Set<String> allowedKeys, String allowedText) {
        if (!entry.contains(KEY)) {
            return Optional.empty();
        }
        ConfigurationSection effects = entry.getConfigurationSection(KEY);
        if (effects == null) {
            errors.add(entry.getCurrentPath() + "." + KEY + ": must be a section with " + allowedText);
            return Optional.empty();
        }
        for (String key : effects.getKeys(false)) {
            if (!allowedKeys.contains(key)) {
                errors.add(effects.getCurrentPath() + "." + key + ": unknown effect, " + entry.getParent().getName()
                        + " can only have " + allowedText);
            }
        }
        return Optional.of(effects);
    }
}
