package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

// Reads skills.yml into a SkillsConfig. Like stats.yml, every mistake is collected
// with its path ("skills.farming.setup.slot: ...") and reported together in one
// DefinitionException; nothing is half-loaded. Checks that need more than one
// entry (aliases and slots used twice, book pages) are in SkillsValidator.
public final class SkillsConfigLoader {
    private static final String FILE_NAME = "skills.yml";

    private final List<String> errors = new ArrayList<>();
    private final ConfigReader reader;

    private SkillsConfigLoader(Predicate<String> isItemMaterial) {
        this.reader = new ConfigReader(errors, isItemMaterial);
    }

    public static SkillsConfig load(ConfigurationSection root, Predicate<String> isItemMaterial) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(isItemMaterial, "isItemMaterial");
        return new SkillsConfigLoader(isItemMaterial).read(root);
    }

    private SkillsConfig read(ConfigurationSection root) {
        SkillLevels levels = readLevels(root);
        SkillSetupRules setup = readSetupRules(root, levels);
        List<SkillBookPage> book = readBook(root);
        Map<String, SkillDefinition> skills = readSkills(root);
        new SkillsValidator(skills, book, setup, errors).validate();
        if (!errors.isEmpty()) {
            throw new DefinitionException(FILE_NAME, errors);
        }
        return new SkillsConfig(levels, setup, book, new SkillDefinitions(skills.values()));
    }

    private SkillLevels readLevels(ConfigurationSection root) {
        List<SkillLevel> levels = new ArrayList<>();
        ConfigurationSection section = root.getConfigurationSection("levels");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                readEntry(section, id).ifPresent(level -> levels.add(readLevel(id, level)));
            }
        }
        if (levels.isEmpty()) {
            errors.add("levels: missing, skills.yml needs at least one level");
            return new SkillLevels(List.of(new SkillLevel("none", "", "", 0)));
        }
        checkLevelOrder(levels);
        return new SkillLevels(levels);
    }

    private SkillLevel readLevel(String id, ConfigurationSection level) {
        return new SkillLevel(id, level.getString("name", id), level.getString("color", ""),
                reader.readNonNegative(level, "xp"));
    }

    private void checkLevelOrder(List<SkillLevel> levels) {
        if (levels.getFirst().xp() != 0) {
            errors.add("levels." + levels.getFirst().id() + ".xp: the first level must start at 0 XP");
        }
        for (int i = 1; i < levels.size(); i++) {
            SkillLevel previous = levels.get(i - 1);
            SkillLevel level = levels.get(i);
            if (level.xp() <= previous.xp()) {
                errors.add("levels." + level.id() + ".xp: must be more than " + previous.id() + "'s " + previous.xp()
                        + ", levels go from lowest to highest");
            }
        }
    }

    private SkillSetupRules readSetupRules(ConfigurationSection root, SkillLevels levels) {
        SkillLevel first = levels.all().getFirst();
        ConfigurationSection setup = root.getConfigurationSection("setup");
        if (setup == null) {
            return new SkillSetupRules(0, first);
        }
        String levelId = ConfigReader.lowercase(List.of(setup.getString("picked-level", first.id()))).getFirst();
        SkillLevel picked = levels.get(levelId).orElse(null);
        if (picked == null) {
            errors.add("setup.picked-level: '" + levelId + "' is not a level id");
            picked = first;
        }
        return new SkillSetupRules(reader.readNonNegative(setup, "picks"), picked);
    }

    private List<SkillBookPage> readBook(ConfigurationSection root) {
        List<SkillBookPage> pages = new ArrayList<>();
        ConfigurationSection book = root.getConfigurationSection("book");
        if (book == null) {
            return pages;
        }
        for (String id : book.getKeys(false)) {
            readEntry(book, id).ifPresent(page -> pages.add(new SkillBookPage(id, page.getString("title", ""),
                    ConfigReader.lowercase(page.getStringList("skills")))));
        }
        return pages;
    }

    private Map<String, SkillDefinition> readSkills(ConfigurationSection root) {
        Map<String, SkillDefinition> skills = new LinkedHashMap<>();
        ConfigurationSection section = root.getConfigurationSection("skills");
        if (section == null) {
            errors.add("skills: missing, skills.yml must have a 'skills:' section");
            return skills;
        }
        for (String id : section.getKeys(false)) {
            readEntry(section, id).ifPresent(skill -> skills.put(id, readSkill(id, skill)));
        }
        return skills;
    }

    private SkillDefinition readSkill(String id, ConfigurationSection skill) {
        String name = skill.getString("name", id);
        return new SkillDefinition(id, name, skill.getString("short-name", name), skill.getString("color", ""),
                ConfigReader.lowercase(skill.getStringList("aliases")), reader.readNonNegative(skill, "baseline"),
                readSetup(skill));
    }

    // Returns the /setup settings, or null when the skill can't be picked in /setup.
    private SkillSetup readSetup(ConfigurationSection skill) {
        ConfigurationSection setup = skill.getConfigurationSection("setup");
        if (setup == null) {
            return null;
        }
        int slot = reader.readRequiredInt(setup, "slot");
        if (slot < SkillSetup.FIRST_SLOT || slot > SkillSetup.LAST_SLOT) {
            errors.add(setup.getCurrentPath() + ".slot: must be between " + SkillSetup.FIRST_SLOT + " and "
                    + SkillSetup.LAST_SLOT + ", the top and bottom rows hold the border, header and confirm button");
        }
        return new SkillSetup(reader.readIcon(setup), slot, setup.getString("color", ""), setup.getStringList("lore"));
    }

    // The settings section of one entry (a level, a book page or a skill), checking
    // its id. Empty when the entry is broken; its error is already added.
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
