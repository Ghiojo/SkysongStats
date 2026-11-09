package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads the "modifiers:" of an effects: section: one string of entries split by
// commas ("str-1, dex+2") or a YAML list of them. Each entry is
// <target><+ or -><amount>, where target is anything /modifier add accepts (a stat
// id or alias, or max_<bar>) and amount a whole number other than 0, or $ for
// the condition's level when levels are allowed. Every bad entry is added to
// errors with its path, and left out.
final class EffectModifierParser {
    // The target is matched lazily, so a stat id with '-' in it ("temp-hp-1")
    // still splits at the last sign.
    private static final Pattern ENTRY = Pattern.compile("([a-z0-9_-]+?)([+-])(\\d+|\\$)");
    private static final String LEVEL = "$";

    private final StatDefinitions stats;
    private final List<String> errors;

    EffectModifierParser(StatDefinitions stats, List<String> errors) {
        this.stats = stats;
        this.errors = errors;
    }

    // allowLevel: whether $ may be used (conditions.yml) or not (injuries.yml).
    List<EffectModifier> read(ConfigurationSection effects, boolean allowLevel) {
        String where = effects.getCurrentPath() + ".modifiers";
        List<EffectModifier> modifiers = new ArrayList<>();
        for (String entry : entries(effects, where)) {
            parse(entry, where, allowLevel).ifPresent(modifiers::add);
        }
        return List.copyOf(modifiers);
    }

    private List<String> entries(ConfigurationSection effects, String where) {
        if (!effects.contains("modifiers")) {
            return List.of();
        }
        if (effects.isList("modifiers")) {
            List<String> entries = new ArrayList<>();
            effects.getStringList("modifiers").forEach(item -> entries.addAll(split(item)));
            return entries;
        }
        if (effects.isString("modifiers")) {
            return split(effects.getString("modifiers"));
        }
        errors.add(where + ": must be a text like \"str-1, dex+2\" or a list of such entries");
        return List.of();
    }

    private static List<String> split(String text) {
        List<String> entries = new ArrayList<>();
        for (String part : text.split(",")) {
            String entry = part.replaceAll("\\s+", "");
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private Optional<EffectModifier> parse(String entry, String where, boolean allowLevel) {
        Matcher matcher = ENTRY.matcher(entry.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            errors.add(where + ": '" + entry + "' must be <stat><+ or -><amount>, for example str-1 or max_hp+5");
            return Optional.empty();
        }
        Optional<String> target = target(matcher.group(1));
        if (target.isEmpty()) {
            errors.add(where + ": '" + entry + "': '" + matcher.group(1)
                    + "' is not a stat id or alias in stats.yml, or max_<bar>");
            return Optional.empty();
        }
        int sign = matcher.group(2).equals("-") ? -1 : 1;
        return amount(matcher.group(3), entry, where, allowLevel)
                .map(amount -> new EffectModifier(target.get(), sign * amount, LEVEL.equals(matcher.group(3))));
    }

    private Optional<Integer> amount(String text, String entry, String where, boolean allowLevel) {
        if (LEVEL.equals(text)) {
            if (!allowLevel) {
                errors.add(where + ": '" + entry + "': $ (the level) only works in conditions.yml, injuries have no level");
                return Optional.empty();
            }
            return Optional.of(1);
        }
        int amount;
        try {
            amount = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            errors.add(where + ": '" + entry + "': the amount is too big");
            return Optional.empty();
        }
        if (amount == 0) {
            errors.add(where + ": '" + entry + "': an amount of 0 does nothing, remove the entry");
            return Optional.empty();
        }
        return Optional.of(amount);
    }

    // A stat id or alias gives the stat's id; max_<bar> stays as it is, like /modifier add.
    private Optional<String> target(String name) {
        Optional<String> stat = stats.find(name).map(StatDefinition::id);
        if (stat.isPresent()) {
            return stat;
        }
        return stats.findBarByMaxId(name).flatMap(StatDefinition::maxId);
    }
}
