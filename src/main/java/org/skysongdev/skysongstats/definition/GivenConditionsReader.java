package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// Reads the "conditions:" of an effects: section, in injuries.yml or
// conditions.yml: a list of condition ids, each with an optional :level
// ("bleeding:2"); a condition without one is level 1. Each bad entry is added
// to errors with its path, and left out.
final class GivenConditionsReader {
    static final String KEY = "conditions";
    private static final Pattern LEVEL = Pattern.compile("\\d{1,9}");

    private final Predicate<String> isCondition;
    private final List<String> errors;

    // isCondition tells whether an id is a condition in conditions.yml.
    GivenConditionsReader(Predicate<String> isCondition, List<String> errors) {
        this.isCondition = isCondition;
        this.errors = errors;
    }

    // Condition id -> level, in the order listed.
    Map<String, Integer> read(ConfigurationSection effects) {
        Map<String, Integer> given = new LinkedHashMap<>();
        if (!effects.contains(KEY)) {
            return given;
        }
        String where = effects.getCurrentPath() + "." + KEY;
        if (!effects.isList(KEY)) {
            errors.add(where + ": must be a list of condition ids, each with an optional :level, like [bleeding, \"weakened:2\"]");
            return given;
        }
        for (String entry : effects.getStringList(KEY)) {
            String id = conditionId(entry);
            readLevel(entry, id, where).ifPresent(level -> {
                if (given.put(id, level) != null) {
                    errors.add(where + ": '" + id + "' is listed twice");
                }
            });
        }
        return given;
    }

    private Optional<Integer> readLevel(String entry, String id, String where) {
        if (!isCondition.test(id)) {
            errors.add(where + ": '" + id + "' is not a condition id in conditions.yml");
            return Optional.empty();
        }
        int colon = entry.indexOf(':');
        if (colon < 0) {
            return Optional.of(1);
        }
        String levelText = entry.substring(colon + 1).trim();
        if (LEVEL.matcher(levelText).matches() && Integer.parseInt(levelText) >= 1) {
            return Optional.of(Integer.parseInt(levelText));
        }
        errors.add(where + ": '" + entry.trim() + "': the level after ':' must be a whole number of 1 or more");
        return Optional.empty();
    }

    private static String conditionId(String entry) {
        int colon = entry.indexOf(':');
        return ConfigReader.lowercase(List.of(colon < 0 ? entry : entry.substring(0, colon))).getFirst();
    }
}
