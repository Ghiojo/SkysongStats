package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

// Reads injuries.yml into InjuryDefinitions. Like the other definition files,
// every mistake is collected with its path ("injuries.broken-leg.severity: ...")
// and reported together in one DefinitionException; nothing is half-loaded.
// Effect modifiers are checked against stats.yml, and the conditions an injury
// gives against conditions.yml, both as loaded now.
public final class InjuriesConfigLoader {
    static final String FILE_NAME = "injuries.yml";
    private static final Set<String> EFFECT_KEYS = Set.of("modifiers", GivenConditionsReader.KEY);
    private static final String EFFECTS_TEXT = "modifiers and conditions";

    private final List<String> errors = new ArrayList<>();
    private final ConfigReader reader;
    private final EffectModifierParser modifiers;
    private final GivenConditionsReader givenConditions;

    private InjuriesConfigLoader(StatDefinitions stats, ConditionDefinitions conditions,
                                 Predicate<String> isItemMaterial) {
        this.reader = new ConfigReader(errors, isItemMaterial);
        this.modifiers = new EffectModifierParser(stats, errors);
        this.givenConditions = new GivenConditionsReader(condition -> conditions.get(condition).isPresent(), errors);
    }

    // isItemMaterial tells whether an icon names an item Material; the plugin asks
    // the server, tests can accept any name.
    public static InjuryDefinitions load(ConfigurationSection root, StatDefinitions stats,
                                         ConditionDefinitions conditions, Predicate<String> isItemMaterial) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(conditions, "conditions");
        Objects.requireNonNull(isItemMaterial, "isItemMaterial");
        return new InjuriesConfigLoader(stats, conditions, isItemMaterial).read(root);
    }

    private InjuryDefinitions read(ConfigurationSection root) {
        List<String> severities = readSeverities(root);
        Map<String, InjuryDefinition> injuries = new LinkedHashMap<>();
        ConfigurationSection section = root.getConfigurationSection("injuries");
        if (section == null) {
            errors.add("injuries: missing, injuries.yml must have an 'injuries:' section ('injuries: {}' for none)");
        } else {
            for (String id : section.getKeys(false)) {
                readInjury(section, id, severities).ifPresent(injury -> injuries.put(id, injury));
            }
        }
        if (!errors.isEmpty()) {
            throw new DefinitionException(FILE_NAME, errors);
        }
        return new InjuryDefinitions(severities, injuries.values());
    }

    private List<String> readSeverities(ConfigurationSection root) {
        if (!root.isList("severities")) {
            errors.add("severities: " + (root.contains("severities") ? "must be a list" : "missing")
                    + ", the severities injuries can have, for example [minor, moderate, severe, permanent]");
            return List.of();
        }
        Set<String> severities = new LinkedHashSet<>();
        for (String severity : ConfigReader.lowercase(root.getStringList("severities"))) {
            if (!StatsConfigLoader.ID.matcher(severity).matches()) {
                errors.add("severities: '" + severity + "' may only use lowercase letters, digits, '_' and '-'");
            } else if (!severities.add(severity)) {
                errors.add("severities: '" + severity + "' is listed twice");
            }
        }
        return List.copyOf(severities);
    }

    private Optional<InjuryDefinition> readInjury(ConfigurationSection parent, String id, List<String> severities) {
        ConfigurationSection injury = parent.getConfigurationSection(id);
        String path = parent.getCurrentPath() + "." + id;
        if (injury == null) {
            errors.add(path + ": must be a section with its settings");
            return Optional.empty();
        }
        if (!StatsConfigLoader.ID.matcher(id).matches()) {
            errors.add(path + ": ids may only use lowercase letters, digits, '_' and '-'");
            return Optional.empty();
        }
        Icon icon = injury.contains("icon") ? reader.readIcon(injury) : Icon.parse(ConditionsConfigLoader.DEFAULT_ICON);
        Optional<ConfigurationSection> effects = new EffectsReader(errors).open(injury, EFFECT_KEYS, EFFECTS_TEXT);
        return Optional.of(new InjuryDefinition(id, injury.getString("name", id), injury.getString("description", ""),
                icon, readSeverity(injury, severities), injury.getString("message", ""),
                effects.map(section -> modifiers.read(section, false)).orElse(List.of()),
                effects.map(givenConditions::read).orElse(Map.of())));
    }

    private String readSeverity(ConfigurationSection injury, List<String> severities) {
        String severity = injury.getString("severity");
        String where = injury.getCurrentPath() + ".severity";
        if (severity == null) {
            errors.add(where + ": missing, one of " + severities);
            return "";
        }
        String normalized = ConfigReader.lowercase(List.of(severity)).getFirst();
        if (!severities.contains(normalized)) {
            errors.add(where + ": '" + severity + "' is not in severities " + severities);
        }
        return normalized;
    }
}
