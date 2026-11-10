package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;
import org.skysongdev.skysongstats.api.SheetField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

// Reads ancestries.yml into AncestryDefinitions. Like the other definition files,
// every mistake is collected with its path ("ancestries.elf.slot: ...") and
// reported together in one DefinitionException; nothing is half-loaded.
// Adjustments may only name the stats with a setup: section in stats.yml (as
// loaded now), by id or alias; they are stored by id.
//
// choices: is a list, so each choice (and each package of a package choice) is
// read from a plain map, with its path written as choices[1], options[2]...
public final class AncestriesConfigLoader {
    static final String FILE_NAME = "ancestries.yml";
    // The /setup ancestry list is 54 slots: the top row holds its header, the
    // bottom row its border.
    static final int FIRST_SLOT = 9;
    static final int LAST_SLOT = 44;
    // The choices page has one row per choice: the first slot explains it, the
    // other eight hold what it can pick.
    static final int MAX_CHOICES = 5;
    static final int MAX_CHOICE_ITEMS = 8;
    private static final String ANY = "any";
    private static final String PACKAGE_ICON = "PAPER";
    private static final Set<String> STAT_PICK_KEYS = Set.of("name", "amount", "picks", "from", "stack");
    private static final Set<String> PACKAGE_KEYS = Set.of("name", "options", "stack");
    private static final Set<String> OPTION_KEYS = Set.of("name", "icon", "stats");

    private final List<String> errors = new ArrayList<>();
    private final ConfigReader reader;
    private final StatDefinitions stats;

    private AncestriesConfigLoader(StatDefinitions stats, Predicate<String> isItemMaterial) {
        this.stats = stats;
        this.reader = new ConfigReader(errors, isItemMaterial);
    }

    // isItemMaterial tells whether an icon names an item Material; the plugin asks
    // the server, tests can accept any name.
    public static AncestryDefinitions load(ConfigurationSection root, StatDefinitions stats,
                                           Predicate<String> isItemMaterial) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(isItemMaterial, "isItemMaterial");
        return new AncestriesConfigLoader(stats, isItemMaterial).read(root);
    }

    private AncestryDefinitions read(ConfigurationSection root) {
        List<AncestryDefinition> ancestries = new ArrayList<>();
        ConfigurationSection section = root.getConfigurationSection("ancestries");
        if (section == null) {
            errors.add("ancestries: missing, ancestries.yml must have an 'ancestries:' section ('ancestries: {}' for none)");
        } else {
            Map<Integer, String> slots = new LinkedHashMap<>();
            for (String id : section.getKeys(false)) {
                readAncestry(section, id, slots).ifPresent(ancestries::add);
            }
        }
        if (!errors.isEmpty()) {
            throw new DefinitionException(FILE_NAME, errors);
        }
        return new AncestryDefinitions(ancestries);
    }

    private Optional<AncestryDefinition> readAncestry(ConfigurationSection parent, String id,
                                                      Map<Integer, String> slots) {
        ConfigurationSection ancestry = parent.getConfigurationSection(id);
        String path = parent.getCurrentPath() + "." + id;
        if (ancestry == null) {
            errors.add(path + ": must be a section with its settings");
            return Optional.empty();
        }
        if (!StatsConfigLoader.ID.matcher(id).matches()) {
            errors.add(path + ": ids may only use lowercase letters, digits, '_' and '-'");
            return Optional.empty();
        }
        String name = readName(ancestry, id);
        Icon icon = reader.readIcon(ancestry);
        int slot = readSlot(ancestry, id, slots);
        List<String> lore = ancestry.getStringList("lore");
        Optional<String> permission = readPermission(ancestry);
        List<AncestryAdjustment> adjustments = readAdjustments(path + ".stats", ancestry.get("stats"));
        List<AncestryChoice> choices = readChoices(ancestry, adjustments);
        return Optional.of(new AncestryDefinition(id, name, icon, slot, lore, permission, adjustments, choices));
    }

    // The name also fills the character's ancestry field, so it must fit there.
    private String readName(ConfigurationSection ancestry, String id) {
        String name = ancestry.getString("name", id);
        int max = SheetField.ANCESTRY.maxLength();
        if (name.isBlank()) {
            errors.add(ancestry.getCurrentPath() + ".name: can't be blank");
        } else if (name.length() > max) {
            errors.add(ancestry.getCurrentPath() + ".name: is " + name.length() + " characters, at most " + max
                    + " fit the character's ancestry field");
        }
        return name;
    }

    private int readSlot(ConfigurationSection ancestry, String id, Map<Integer, String> slots) {
        int slot = reader.readRequiredInt(ancestry, "slot");
        String where = ancestry.getCurrentPath() + ".slot";
        if (!ancestry.isInt("slot")) {
            return slot;
        }
        if (slot < FIRST_SLOT || slot > LAST_SLOT) {
            errors.add(where + ": " + slot + " is outside the ancestry list, use " + FIRST_SLOT + "-" + LAST_SLOT);
        } else {
            String taken = slots.putIfAbsent(slot, id);
            if (taken != null) {
                errors.add(where + ": " + slot + " is already used by ancestry '" + taken + "'");
            }
        }
        return slot;
    }

    private Optional<String> readPermission(ConfigurationSection ancestry) {
        if (!ancestry.contains("permission")) {
            return Optional.empty();
        }
        String permission = ancestry.getString("permission", "").trim();
        if (permission.isEmpty() || permission.chars().anyMatch(Character::isWhitespace)) {
            errors.add(ancestry.getCurrentPath() + ".permission: '" + ancestry.get("permission")
                    + "' must be one permission node without spaces, or left out for everyone");
            return Optional.empty();
        }
        return Optional.of(permission);
    }

    // stat: amount pairs, from a section (stats:) or a map inside a list (a
    // package's stats:). Missing means no adjustments.
    private List<AncestryAdjustment> readAdjustments(String where, Object value) {
        if (value == null) {
            return List.of();
        }
        Optional<Map<String, Object>> read = entries(value);
        if (read.isEmpty()) {
            errors.add(where + ": must be stat: amount pairs, for example {con: 1, spd: -1}");
            return List.of();
        }
        Map<String, Object> entries = read.get();
        Map<String, AncestryAdjustment> adjustments = new LinkedHashMap<>();
        entries.forEach((key, amount) -> {
            String at = where + "." + key;
            Optional<String> stat = setupStat(at, key);
            if (!(amount instanceof Integer number) || number == 0) {
                errors.add(at + ": '" + amount + "' must be a whole number other than 0 (negative takes off)");
            } else if (stat.isPresent() && adjustments.putIfAbsent(stat.get(), new AncestryAdjustment(stat.get(), number)) != null) {
                errors.add(at + ": adjusts '" + stat.get() + "' a second time");
            }
        });
        return List.copyOf(adjustments.values());
    }

    private List<AncestryChoice> readChoices(ConfigurationSection ancestry, List<AncestryAdjustment> fixed) {
        String where = ancestry.getCurrentPath() + ".choices";
        if (!ancestry.contains("choices")) {
            return List.of();
        }
        if (!ancestry.isList("choices")) {
            errors.add(where + ": must be a list of choices");
            return List.of();
        }
        List<?> list = ancestry.getList("choices", List.of());
        if (list.size() > MAX_CHOICES) {
            errors.add(where + ": has " + list.size() + " choices, at most " + MAX_CHOICES
                    + " fit the choices page (one row each)");
        }
        List<AncestryChoice> choices = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            readChoice(where + "[" + (i + 1) + "]", list.get(i), fixed).ifPresent(choices::add);
        }
        return choices;
    }

    private Optional<AncestryChoice> readChoice(String where, Object value, List<AncestryAdjustment> fixed) {
        Optional<Map<String, Object>> read = entries(value);
        if (read.isEmpty()) {
            errors.add(where + ": must be a choice with its settings");
            return Optional.empty();
        }
        Map<String, Object> choice = read.get();
        boolean isPackage = choice.containsKey("options");
        Set<String> allowed = isPackage ? PACKAGE_KEYS : STAT_PICK_KEYS;
        for (String key : choice.keySet()) {
            if (!allowed.contains(key)) {
                errors.add(where + "." + key + ": " + (isPackage
                        ? "a choice with options: is a package choice and only takes " + PACKAGE_KEYS
                        : "unknown setting, a choice takes " + STAT_PICK_KEYS + " or name + options"));
            }
        }
        String name = text(choice.get("name"), "");
        boolean stack = readStack(where, choice.get("stack"));
        return isPackage ? readPackageChoice(where, name, choice.get("options"), stack, fixed)
                : readStatPickChoice(where, name, choice, stack, fixed);
    }

    private Optional<AncestryChoice> readStatPickChoice(String where, String name, Map<String, Object> choice,
                                                        boolean stack, List<AncestryAdjustment> fixed) {
        Object amount = choice.get("amount");
        if (!(amount instanceof Integer number) || number == 0) {
            errors.add(where + ".amount: " + (amount == null ? "missing" : "'" + amount + "'")
                    + ", must be a whole number other than 0 (negative takes off)");
            return Optional.empty();
        }
        Object picksValue = choice.getOrDefault("picks", 1);
        if (!(picksValue instanceof Integer picks) || picks < 1) {
            errors.add(where + ".picks: '" + picksValue + "' must be a whole number of at least 1");
            return Optional.empty();
        }
        Object from = choice.get("from");
        boolean anyStat = from instanceof String text && text.trim().equalsIgnoreCase(ANY);
        List<String> picked = anyStat ? stats.setupStats().stream().map(StatDefinition::id).toList()
                : readFrom(where + ".from", from);
        if (picked.isEmpty()) {
            return Optional.empty();
        }
        if (picked.size() > MAX_CHOICE_ITEMS) {
            errors.add(where + ".from: has " + picked.size() + " stats, at most " + MAX_CHOICE_ITEMS
                    + " fit a row of the choices page");
        }
        if (picks > picked.size()) {
            errors.add(where + ".picks: " + picks + " is more than the " + picked.size() + " stats it picks from");
            return Optional.empty();
        }
        long open = picked.stream().filter(stat -> stack || !movesSameWay(fixed, stat, number)).count();
        if (open < picks) {
            errors.add(where + ": can never be completed, only " + open + " of its stats aren't already "
                    + (number > 0 ? "raised" : "lowered") + " by this ancestry's stats: (add stack: true to allow it)");
        }
        return Optional.of(new StatPickChoice(name, number, picks, picked, anyStat, stack));
    }

    private List<String> readFrom(String where, Object from) {
        if (!(from instanceof List<?> names) || names.isEmpty()) {
            errors.add(where + ": " + (from == null ? "missing" : "'" + from + "'")
                    + ", must be 'any' or a list of stat ids, for example [str, dex]");
            return List.of();
        }
        Set<String> stats = new LinkedHashSet<>();
        boolean valid = true;
        for (Object name : names) {
            Optional<String> stat = setupStat(where, String.valueOf(name));
            if (stat.isEmpty()) {
                valid = false;
            } else if (!stats.add(stat.get())) {
                errors.add(where + ": lists '" + stat.get() + "' twice");
                valid = false;
            }
        }
        return valid ? List.copyOf(stats) : List.of();
    }

    private Optional<AncestryChoice> readPackageChoice(String where, String name, Object value, boolean stack,
                                                       List<AncestryAdjustment> fixed) {
        if (!(value instanceof List<?> list) || list.size() < 2) {
            errors.add(where + ".options: must be a list of at least two packages to pick one from");
            return Optional.empty();
        }
        if (list.size() > MAX_CHOICE_ITEMS) {
            errors.add(where + ".options: has " + list.size() + " packages, at most " + MAX_CHOICE_ITEMS
                    + " fit a row of the choices page");
        }
        List<AncestryPackage> options = new ArrayList<>();
        int errorsBefore = errors.size();
        for (int i = 0; i < list.size(); i++) {
            readPackage(where + ".options[" + (i + 1) + "]", list.get(i), stack, fixed).ifPresent(options::add);
        }
        if (errors.size() > errorsBefore) {
            return Optional.empty();
        }
        return Optional.of(new PackageChoice(name, options, stack));
    }

    private Optional<AncestryPackage> readPackage(String where, Object value, boolean stack,
                                                  List<AncestryAdjustment> fixed) {
        Optional<Map<String, Object>> read = entries(value);
        if (read.isEmpty()) {
            errors.add(where + ": must be a package with a name and stats:");
            return Optional.empty();
        }
        Map<String, Object> option = read.get();
        option.keySet().stream().filter(key -> !OPTION_KEYS.contains(key)).forEach(key ->
                errors.add(where + "." + key + ": unknown setting, a package takes " + OPTION_KEYS));
        String name = text(option.get("name"), "");
        if (name.isBlank()) {
            errors.add(where + ".name: missing, the package's name on the choices page");
        }
        Icon icon = option.containsKey("icon") ? reader.readIcon(where + ".icon", text(option.get("icon"), null))
                : Icon.parse(PACKAGE_ICON);
        if (!option.containsKey("stats")) {
            errors.add(where + ".stats: missing, what the package gives, for example {str: 2}");
            return Optional.empty();
        }
        List<AncestryAdjustment> adjustments = readAdjustments(where + ".stats", option.get("stats"));
        if (adjustments.isEmpty()) {
            return Optional.empty();
        }
        if (!stack) {
            adjustments.stream().filter(adjustment -> movesSameWay(fixed, adjustment.stat(), adjustment.amount()))
                    .forEach(adjustment -> errors.add(where + ": can never be picked, this ancestry's stats: already "
                            + (adjustment.isBoost() ? "raise" : "lower") + " '" + adjustment.stat()
                            + "' (add stack: true to the choice to allow it)"));
        }
        return Optional.of(new AncestryPackage(name, icon, adjustments));
    }

    private boolean readStack(String where, Object value) {
        if (value == null) {
            return false;
        }
        if (!(value instanceof Boolean stack)) {
            errors.add(where + ".stack: '" + value + "' must be true or false");
            return false;
        }
        return stack;
    }

    // A /setup stat by id or alias, as its id. Other stats aren't picked in /setup,
    // so an ancestry can't adjust them.
    private Optional<String> setupStat(String where, String name) {
        Optional<StatDefinition> stat = stats.find(name);
        if (stat.isEmpty()) {
            errors.add(where + ": '" + name + "' is not a stat in stats.yml");
            return Optional.empty();
        }
        if (stat.get().setup() == null) {
            errors.add(where + ": '" + name + "' has no setup: section in stats.yml, ancestries only adjust /setup stats");
            return Optional.empty();
        }
        return Optional.of(stat.get().id());
    }

    private static boolean movesSameWay(List<AncestryAdjustment> fixed, String stat, int amount) {
        return fixed.stream().anyMatch(adjustment -> adjustment.stat().equals(stat)
                && adjustment.isBoost() == (amount > 0));
    }

    // The entries of a section or of a map read from a list, by key; empty when
    // the value is neither.
    private static Optional<Map<String, Object>> entries(Object value) {
        if (value instanceof ConfigurationSection section) {
            return Optional.of(section.getValues(false));
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> entries = new LinkedHashMap<>();
            map.forEach((key, entry) -> entries.put(String.valueOf(key), entry));
            return Optional.of(entries);
        }
        return Optional.empty();
    }

    private static String text(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
