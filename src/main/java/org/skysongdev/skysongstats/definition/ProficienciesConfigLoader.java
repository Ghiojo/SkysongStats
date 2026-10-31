package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

// Reads proficiencies.yml into a ProficienciesConfig. Like skills.yml, every
// mistake is collected with its path ("proficiencies.axes.skill: ...") and
// reported together in one DefinitionException; nothing is half-loaded. Checks
// that need more than one entry, or skills.yml, are in ProficienciesValidator.
public final class ProficienciesConfigLoader {
    private static final String FILE_NAME = "proficiencies.yml";

    private final List<String> errors = new ArrayList<>();
    private final ConfigReader reader;

    private ProficienciesConfigLoader(Predicate<String> isItemMaterial) {
        this.reader = new ConfigReader(errors, isItemMaterial);
    }

    // isItemMaterial tells whether a /setup icon names an item Material; the
    // plugin asks the server, tests can accept any name.
    public static ProficienciesConfig load(ConfigurationSection root, SkillDefinitions skills,
                                           Predicate<String> isItemMaterial) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(isItemMaterial, "isItemMaterial");
        return new ProficienciesConfigLoader(isItemMaterial).read(root, skills);
    }

    private ProficienciesConfig read(ConfigurationSection root, SkillDefinitions skills) {
        ProficiencyTiers tiers = readTiers(root);
        int cost = readCost(root, "cost");
        Map<String, ProficiencyDefinition> proficiencies = readProficiencies(root, cost);
        ProficiencySetupRules setup = readSetupRules(root);
        new ProficienciesValidator(proficiencies, skills, errors).validate(setup);
        if (!errors.isEmpty()) {
            throw new DefinitionException(FILE_NAME, errors);
        }
        return new ProficienciesConfig(tiers, new ProficiencyDefinitions(proficiencies.values()), setup);
    }

    // No setup: section gives new characters nothing, and /setup skips the screen.
    private ProficiencySetupRules readSetupRules(ConfigurationSection root) {
        ConfigurationSection setup = root.getConfigurationSection("setup");
        if (setup == null) {
            if (root.contains("setup")) {
                errors.add("setup: must be a section with trained, picks and skill-picks");
            }
            return ProficiencySetupRules.none();
        }
        return new ProficiencySetupRules(ConfigReader.lowercase(setup.getStringList("trained")),
                readPicks(setup, "picks"), readPicks(setup, "skill-picks"));
    }

    // A section of skill id: number of picks.
    private Map<String, Integer> readPicks(ConfigurationSection setup, String key) {
        Map<String, Integer> picks = new LinkedHashMap<>();
        ConfigurationSection section = setup.getConfigurationSection(key);
        if (section == null) {
            if (setup.contains(key)) {
                errors.add(setup.getCurrentPath() + "." + key + ": must be a section of skill id: number of picks");
            }
            return picks;
        }
        for (String skill : section.getKeys(false)) {
            picks.put(ConfigReader.lowercase(List.of(skill)).getFirst(), reader.readNonNegative(section, skill));
        }
        return picks;
    }

    private ProficiencyTiers readTiers(ConfigurationSection root) {
        List<ProficiencyTier> tiers = new ArrayList<>();
        ConfigurationSection section = root.getConfigurationSection("tiers");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                readEntry(section, id).ifPresent(tier -> tiers.add(readTier(id, tier, tiers.size())));
            }
        }
        if (tiers.size() < 2) {
            errors.add("tiers: proficiencies.yml needs at least two tiers, the starting one and one to train to");
            return new ProficiencyTiers(List.of(new ProficiencyTier("none", "", "", 0),
                    new ProficiencyTier("trained", "", "", 0)));
        }
        return new ProficiencyTiers(tiers);
    }

    // The first tier is free and the second costs the global or proficiency cost,
    // so only the tiers after those can have an extra.
    private ProficiencyTier readTier(String id, ConfigurationSection tier, int position) {
        if (position < 2 && tier.contains("extra")) {
            errors.add(tier.getCurrentPath() + ".extra: only the tiers after the first two have an extra; "
                    + (position == 0 ? "the first tier is free" : "this tier costs 'cost'"));
        }
        return new ProficiencyTier(id, tier.getString("name", id), tier.getString("color", ""),
                reader.readNonNegative(tier, "extra"));
    }

    // A cost is required at the top of the file and optional per proficiency; the
    // caller only asks for the second when it is there.
    private int readCost(ConfigurationSection section, String key) {
        String where = section.getCurrentPath().isEmpty() ? key : section.getCurrentPath() + "." + key;
        if (!section.isInt(key)) {
            errors.add(where + ": " + (section.contains(key) ? "'" + section.get(key) + "' is not a whole number"
                    : "missing, the points the first paid tier costs"));
            return 1;
        }
        int cost = section.getInt(key);
        if (cost < 1) {
            errors.add(where + ": must be at least 1");
        }
        return cost;
    }

    private Map<String, ProficiencyDefinition> readProficiencies(ConfigurationSection root, int cost) {
        Map<String, ProficiencyDefinition> proficiencies = new LinkedHashMap<>();
        ConfigurationSection section = root.getConfigurationSection("proficiencies");
        if (section == null) {
            errors.add("proficiencies: missing, proficiencies.yml must have a 'proficiencies:' section");
            return proficiencies;
        }
        for (String id : section.getKeys(false)) {
            readEntry(section, id).ifPresent(proficiency -> proficiencies.put(id, readProficiency(id, proficiency, cost)));
        }
        return proficiencies;
    }

    private ProficiencyDefinition readProficiency(String id, ConfigurationSection proficiency, int globalCost) {
        String skill = proficiency.getString("skill");
        if (skill == null) {
            errors.add(proficiency.getCurrentPath() + ".skill: missing, every proficiency belongs to a skill from skills.yml");
            skill = "";
        }
        int cost = proficiency.contains("cost") ? readCost(proficiency, "cost") : globalCost;
        return new ProficiencyDefinition(id, proficiency.getString("name", id), proficiency.getString("color", ""),
                ConfigReader.lowercase(List.of(skill)).getFirst(),
                ConfigReader.lowercase(proficiency.getStringList("aliases")), cost, readSetup(proficiency));
    }

    private ProficiencySetup readSetup(ConfigurationSection proficiency) {
        Icon icon = proficiency.contains("icon") ? reader.readIcon(proficiency)
                : Icon.parse(ProficiencySetup.DEFAULT_ICON);
        return new ProficiencySetup(icon, proficiency.getStringList("lore"));
    }

    // The settings section of one entry (a tier or a proficiency), checking its id.
    // Empty when the entry is broken; its error is already added.
    private Optional<ConfigurationSection> readEntry(ConfigurationSection parent, String id) {
        String path = parent.getCurrentPath() + "." + id;
        ConfigurationSection entry = parent.getConfigurationSection(id);
        if (entry == null) {
            errors.add(path + ": must be a section with its settings");
            return Optional.empty();
        }
        if (!StatsConfigLoader.ID.matcher(id).matches()) {
            errors.add(path + ": ids may only use lowercase letters, digits, '_' and '-'");
            return Optional.empty();
        }
        return Optional.of(entry);
    }
}
