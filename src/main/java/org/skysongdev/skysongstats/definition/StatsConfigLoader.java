package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;
import org.skysongdev.skysongstats.formula.Formula;
import org.skysongdev.skysongstats.formula.FormulaException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// Reads stats.yml into a StatsConfig. Every mistake in the file is collected, with
// the path where it is ("stats.hp.max: ..."), and reported together in one
// DefinitionException; nothing is half-loaded. Checks that need the other stats
// (formulas, shields, duplicate names) are in StatsValidator.
//
// Reloading is calling load() again with the new file: the result is a new,
// independent StatsConfig.
public final class StatsConfigLoader {
    static final Pattern ID = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern ITEM_TAG = Pattern.compile("[a-z0-9/._-]+");
    private static final int GUI_SLOTS = 27;

    private final Predicate<String> isItemMaterial;
    private final List<String> errors = new ArrayList<>();

    // isItemMaterial tells whether a Material name exists and is an item. It is
    // passed in because Bukkit's Material lookup needs a running server.
    private StatsConfigLoader(Predicate<String> isItemMaterial) {
        this.isItemMaterial = isItemMaterial;
    }

    public static StatsConfig load(ConfigurationSection root, Predicate<String> isItemMaterial) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(isItemMaterial, "isItemMaterial");
        return new StatsConfigLoader(isItemMaterial).read(root);
    }

    private StatsConfig read(ConfigurationSection root) {
        SetupRules rules = readSetupRules(root);
        Map<String, StatDefinition> stats = readStats(root);
        new StatsValidator(stats, errors).validate();
        if (!errors.isEmpty()) {
            throw new DefinitionException("stats.yml", errors);
        }
        return new StatsConfig(rules, new StatDefinitions(stats.values()));
    }

    private SetupRules readSetupRules(ConfigurationSection root) {
        ConfigurationSection setup = root.getConfigurationSection("setup");
        if (setup == null) {
            return new SetupRules(0, 0);
        }
        return new SetupRules(readNonNegative(setup, "points"), readNonNegative(setup, "negative-points"));
    }

    private Map<String, StatDefinition> readStats(ConfigurationSection root) {
        Map<String, StatDefinition> stats = new LinkedHashMap<>();
        ConfigurationSection section = root.getConfigurationSection("stats");
        if (section == null) {
            errors.add("stats: missing, stats.yml must have a 'stats:' section");
            return stats;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection stat = section.getConfigurationSection(id);
            if (stat == null) {
                errors.add(path(id) + ": must be a section with the stat's settings");
            } else if (isValidId(id)) {
                stats.put(id, readStat(id, stat));
            }
        }
        return stats;
    }

    private boolean isValidId(String id) {
        if (!ID.matcher(id).matches()) {
            errors.add(path(id) + ": stat ids may only use lowercase letters, digits, '_' and '-'");
            return false;
        }
        if (id.startsWith(StatDefinition.MAX_PREFIX)) {
            errors.add(path(id) + ": ids starting with '" + StatDefinition.MAX_PREFIX
                    + "' are reserved for bar maximums (bar 'hp' already gives 'max_hp')");
            return false;
        }
        return true;
    }

    private StatDefinition readStat(String id, ConfigurationSection stat) {
        StatDisplay display = new StatDisplay(stat.getString("name", id), stat.getString("color", ""),
                lowercase(stat.getStringList("aliases")));
        BarSettings bar = readType(id, stat);
        return new StatDefinition(id, display, readInt(stat, "baseline", 0), readMin(stat, bar != null),
                bar, readItemTags(stat), readCommands(stat), readSetup(id, stat, bar != null));
    }

    // Returns the bar settings, or null for a plain value.
    private BarSettings readType(String id, ConfigurationSection stat) {
        String type = stat.getString("type", "value").toLowerCase(Locale.ROOT);
        if (type.equals("bar")) {
            return readBar(stat);
        }
        if (!type.equals("value")) {
            errors.add(path(id) + ".type: '" + type + "' is not a stat type, use 'value' or 'bar'");
        }
        for (String barOnly : List.of("max", "start", "shield")) {
            if (stat.contains(barOnly)) {
                errors.add(path(id) + "." + barOnly + ": only bars have '" + barOnly + "' (add 'type: bar')");
            }
        }
        return null;
    }

    private BarSettings readBar(ConfigurationSection stat) {
        Formula max = readFormula(stat);
        Object start = stat.get("start", 0);
        boolean startAtMax = "max".equalsIgnoreCase(String.valueOf(start));
        int startValue = 0;
        if (!startAtMax) {
            startValue = readInt(stat, "start", 0);
        }
        String shield = stat.getString("shield");
        return new BarSettings(max, startAtMax, startValue,
                shield == null ? null : shield.trim().toLowerCase(Locale.ROOT));
    }

    private Formula readFormula(ConfigurationSection stat) {
        Object max = stat.get("max");
        if (max == null) {
            errors.add(stat.getCurrentPath() + ".max: bars need a max, a number or a formula like \"60 + {con} * 10\"");
            return Formula.parse("0");
        }
        try {
            return Formula.parse(String.valueOf(max));
        } catch (FormulaException e) {
            errors.add(stat.getCurrentPath() + ".max: " + e.getMessage());
            return Formula.parse("0");
        }
    }

    private Integer readMin(ConfigurationSection stat, boolean isBar) {
        if (!stat.contains("min")) {
            return isBar ? 0 : null;
        }
        return readInt(stat, "min", 0);
    }

    private List<ItemTagSource> readItemTags(ConfigurationSection stat) {
        List<ItemTagSource> tags = new ArrayList<>();
        String where = stat.getCurrentPath() + ".item-tags";
        for (Map<?, ?> entry : stat.getMapList("item-tags")) {
            Object tag = entry.get("tag");
            String name = tag == null ? "" : String.valueOf(tag).trim().toLowerCase(Locale.ROOT);
            if (!ITEM_TAG.matcher(name).matches()) {
                errors.add(where + ": every entry needs a 'tag' (lowercase letters, digits, '_', '-', '.', '/')");
                continue;
            }
            tags.add(new ItemTagSource(name, Boolean.TRUE.equals(entry.get("requires-charges"))));
        }
        return tags;
    }

    private Map<StatAction, List<String>> readCommands(ConfigurationSection stat) {
        Map<StatAction, List<String>> commands = new EnumMap<>(StatAction.class);
        ConfigurationSection section = stat.getConfigurationSection("commands");
        if (section == null) {
            return commands;
        }
        for (String key : section.getKeys(false)) {
            StatAction action = StatAction.fromConfigName(key).orElse(null);
            if (action == null) {
                errors.add(section.getCurrentPath() + "." + key + ": not an action, use set, add, reduce or damage");
                continue;
            }
            commands.put(action, readCommandNames(section, key));
        }
        return commands;
    }

    private List<String> readCommandNames(ConfigurationSection section, String key) {
        List<String> names = lowercase(section.getStringList(key));
        for (String name : names) {
            if (!ID.matcher(name).matches()) {
                errors.add(section.getCurrentPath() + "." + key + ": '" + name
                        + "' is not a valid command name (lowercase letters, digits, '_' and '-')");
            }
        }
        return names;
    }

    // Returns the /setup settings, or null when the stat doesn't show in /setup.
    private StatSetup readSetup(String id, ConfigurationSection stat, boolean isBar) {
        ConfigurationSection setup = stat.getConfigurationSection("setup");
        if (setup == null) {
            return null;
        }
        if (isBar) {
            errors.add(path(id) + ".setup: bars can't be allocated in /setup, only plain values");
        }
        int slot = readRequiredInt(setup, "slot");
        if (slot < 0 || slot >= GUI_SLOTS) {
            errors.add(setup.getCurrentPath() + ".slot: must be between 0 and " + (GUI_SLOTS - 1));
        }
        int min = readRequiredInt(setup, "min");
        int max = readRequiredInt(setup, "max");
        if (min > 0 || max < 0) {
            errors.add(setup.getCurrentPath() + ": min must be 0 or less and max 0 or more (they are relative to the baseline)");
        }
        return new StatSetup(readIcon(setup), slot, min, max, setup.getStringList("lore"));
    }

    private Icon readIcon(ConfigurationSection section) {
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

    private int readRequiredInt(ConfigurationSection section, String key) {
        if (!section.contains(key)) {
            errors.add(section.getCurrentPath() + "." + key + ": missing, must be a whole number");
            return 0;
        }
        return readInt(section, key, 0);
    }

    private int readNonNegative(ConfigurationSection section, String key) {
        int value = readInt(section, key, 0);
        if (value < 0) {
            errors.add(section.getCurrentPath() + "." + key + ": can't be negative");
        }
        return value;
    }

    private int readInt(ConfigurationSection section, String key, int fallback) {
        if (!section.contains(key)) {
            return fallback;
        }
        if (!section.isInt(key)) {
            errors.add(section.getCurrentPath() + "." + key + ": '" + section.get(key) + "' is not a whole number");
            return fallback;
        }
        return section.getInt(key);
    }

    private static List<String> lowercase(List<String> names) {
        return names.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
    }

    private static String path(String id) {
        return "stats." + id;
    }
}
