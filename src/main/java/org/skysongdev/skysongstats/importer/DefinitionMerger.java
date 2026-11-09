package org.skysongdev.skysongstats.importer;

import org.bukkit.configuration.ConfigurationSection;
import org.skysongdev.skysongstats.definition.ConditionsConfigLoader;
import org.skysongdev.skysongstats.definition.InjuriesConfigLoader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

// Adds SkysongChatTweaks' conditions and injuries to conditions.yml and
// injuries.yml as they are loaded in memory; the importer saves the files after.
// An id the file already has is never touched, so running the import again
// changes nothing and keeps whatever was edited since. Imported entries get no
// effects:, and injuries get message: "" (SkysongChatTweaks had no message).
final class DefinitionMerger {
    // added: the ids written. skipped: each id left out, with why.
    record Result(List<String> added, Map<String, String> skipped) {
        Result {
            added = List.copyOf(added);
            skipped = Map.copyOf(skipped);
        }
    }

    private final Predicate<String> isItemMaterial;

    // isItemMaterial tells whether SkysongChatTweaks' item: can be an icon; one
    // that can't is left out, so the entry gets the default icon.
    DefinitionMerger(Predicate<String> isItemMaterial) {
        this.isItemMaterial = Objects.requireNonNull(isItemMaterial, "isItemMaterial");
    }

    Result mergeConditions(ConfigurationSection root, List<ChatTweaksConfig.Condition> imported) {
        ConfigurationSection conditions = section(root, "conditions");
        List<String> added = new ArrayList<>();
        Map<String, String> skipped = new LinkedHashMap<>();
        for (ChatTweaksConfig.Condition condition : imported) {
            String id = condition.id();
            if (!ConditionsConfigLoader.isValidId(id)) {
                skipped.put(id, "condition ids may only use lowercase letters, digits and '-'");
            } else if (conditions.contains(id)) {
                skipped.put(id, "already in conditions.yml");
            } else {
                ConfigurationSection entry = conditions.createSection(id);
                writeCommon(entry, condition.name(), condition.description(), condition.item());
                added.add(id);
            }
        }
        return new Result(added, skipped);
    }

    Result mergeInjuries(ConfigurationSection root, List<ChatTweaksConfig.Injury> imported) {
        ConfigurationSection injuries = section(root, "injuries");
        List<String> severities = new ArrayList<>(root.getStringList("severities"));
        List<String> added = new ArrayList<>();
        Map<String, String> skipped = new LinkedHashMap<>();
        for (ChatTweaksConfig.Injury injury : imported) {
            String why = whyNot(injuries, injury);
            if (why == null) {
                String severity = addInjury(injuries, injury);
                if (!severities.contains(severity)) {
                    severities.add(severity);
                }
                added.add(injury.id());
            } else {
                skipped.put(injury.id(), why);
            }
        }
        if (!added.isEmpty()) {
            root.set("severities", severities);
        }
        return new Result(added, skipped);
    }

    // Writes the injury and returns its severity, as injuries.yml spells it.
    private String addInjury(ConfigurationSection injuries, ChatTweaksConfig.Injury injury) {
        String severity = injury.severity().toLowerCase(Locale.ROOT);
        ConfigurationSection entry = injuries.createSection(injury.id());
        writeCommon(entry, injury.name(), injury.description(), injury.item());
        entry.set("severity", severity);
        entry.set("message", "");
        return severity;
    }

    // Why the injury can't be added, or null when it can.
    private static String whyNot(ConfigurationSection injuries, ChatTweaksConfig.Injury injury) {
        if (!InjuriesConfigLoader.isValidId(injury.id())) {
            return "injury ids may only use lowercase letters, digits, '_' and '-'";
        }
        if (injuries.contains(injury.id())) {
            return "already in injuries.yml";
        }
        if (injury.severity() == null) {
            return "it has no severity";
        }
        if (!InjuriesConfigLoader.isValidId(injury.severity().toLowerCase(Locale.ROOT))) {
            return "severity '" + injury.severity() + "' may only use letters, digits, '_' and '-'";
        }
        return null;
    }

    private void writeCommon(ConfigurationSection entry, String name, String description, String item) {
        entry.set("name", name);
        entry.set("description", description);
        if (item != null && isItemMaterial.test(item)) {
            entry.set("icon", item);
        }
    }

    private static ConfigurationSection section(ConfigurationSection root, String key) {
        ConfigurationSection section = root.getConfigurationSection(key);
        return section != null ? section : root.createSection(key);
    }
}
