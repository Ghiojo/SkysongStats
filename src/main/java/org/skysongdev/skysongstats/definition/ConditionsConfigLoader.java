package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// Reads conditions.yml into ConditionDefinitions. Like the other definition
// files, every mistake is collected with its path ("conditions.bleeding.icon: ...")
// and reported together in one DefinitionException; nothing is half-loaded.
// Effect modifiers are checked against the stats now loaded from stats.yml. A
// condition can give other conditions, but never in a loop (see ConditionLoops).
public final class ConditionsConfigLoader {
    static final String FILE_NAME = "conditions.yml";
    static final String DEFAULT_ICON = "PAPER";
    // No '_': chat writes a condition with its level as [condition_<id>_<level>],
    // so an id with '_' in it could not be told apart from its level.
    private static final Pattern ID = Pattern.compile("[a-z0-9-]+");
    private static final Set<String> EFFECT_KEYS = Set.of("modifiers", GivenConditionsReader.KEY);
    private static final String EFFECTS_TEXT = "modifiers and conditions (conditions never give injuries)";

    private final List<String> errors = new ArrayList<>();
    private final ConfigReader reader;
    private final EffectModifierParser modifiers;

    private ConditionsConfigLoader(StatDefinitions stats, Predicate<String> isItemMaterial) {
        this.reader = new ConfigReader(errors, isItemMaterial);
        this.modifiers = new EffectModifierParser(stats, errors);
    }

    // isItemMaterial tells whether an icon names an item Material; the plugin asks
    // the server, tests can accept any name.
    public static ConditionDefinitions load(ConfigurationSection root, StatDefinitions stats,
                                            Predicate<String> isItemMaterial) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(isItemMaterial, "isItemMaterial");
        return new ConditionsConfigLoader(stats, isItemMaterial).read(root);
    }

    private ConditionDefinitions read(ConfigurationSection root) {
        Map<String, ConditionDefinition> conditions = new LinkedHashMap<>();
        ConfigurationSection section = root.getConfigurationSection("conditions");
        if (section == null) {
            errors.add("conditions: missing, conditions.yml must have a 'conditions:' section ('conditions: {}' for none)");
        } else {
            // A condition may give any condition of the file, even one further down.
            Set<String> ids = section.getKeys(false);
            GivenConditionsReader given = new GivenConditionsReader(ids::contains, errors);
            for (String id : ids) {
                readCondition(section, id, given).ifPresent(condition -> conditions.put(id, condition));
            }
            ConditionLoops.find(conditions).forEach(loop -> errors.add("conditions: " + loop));
        }
        if (!errors.isEmpty()) {
            throw new DefinitionException(FILE_NAME, errors);
        }
        return new ConditionDefinitions(conditions.values());
    }

    private Optional<ConditionDefinition> readCondition(ConfigurationSection parent, String id,
                                                        GivenConditionsReader given) {
        ConfigurationSection condition = parent.getConfigurationSection(id);
        String path = parent.getCurrentPath() + "." + id;
        if (condition == null) {
            errors.add(path + ": must be a section with its settings");
            return Optional.empty();
        }
        if (!ID.matcher(id).matches()) {
            errors.add(path + ": condition ids may only use lowercase letters, digits and '-'"
                    + " ('_' separates the level in chat: [condition_" + id.replace('_', '-') + "_2])");
            return Optional.empty();
        }
        Icon icon = condition.contains("icon") ? reader.readIcon(condition) : Icon.parse(DEFAULT_ICON);
        Optional<ConfigurationSection> effects = new EffectsReader(errors).open(condition, EFFECT_KEYS, EFFECTS_TEXT);
        return Optional.of(new ConditionDefinition(id, condition.getString("name", id),
                condition.getString("description", ""), icon,
                effects.map(section -> modifiers.read(section, true)).orElse(List.of()),
                effects.map(given::read).orElse(Map.of())));
    }
}
