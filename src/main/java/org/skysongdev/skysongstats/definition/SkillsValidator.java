package org.skysongdev.skysongstats.definition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// The skills.yml checks that look at more than one entry: aliases and /setup
// slots used twice, book pages listing unknown or repeated skills, and more
// /setup picks than pickable skills. Adds each problem to errors.
final class SkillsValidator {
    // Aliases are typed as command arguments, so anything without spaces ("t&e").
    private static final Pattern ALIAS = Pattern.compile("\\S+");

    private final Map<String, SkillDefinition> skills;
    private final List<SkillBookPage> book;
    private final SkillSetupRules setup;
    private final List<String> errors;

    SkillsValidator(Map<String, SkillDefinition> skills, List<SkillBookPage> book, SkillSetupRules setup,
                    List<String> errors) {
        this.skills = skills;
        this.book = book;
        this.setup = setup;
        this.errors = errors;
    }

    void validate() {
        checkAliases();
        checkSetupSlots();
        checkPicks();
        checkBook();
    }

    private void checkAliases() {
        Map<String, String> owners = new HashMap<>();
        skills.keySet().forEach(id -> owners.put(id, id));
        for (SkillDefinition skill : skills.values()) {
            for (String alias : skill.aliases()) {
                checkAlias(skill, alias, owners);
            }
        }
    }

    private void checkAlias(SkillDefinition skill, String alias, Map<String, String> owners) {
        if (!ALIAS.matcher(alias).matches()) {
            errors.add(path(skill) + ".aliases: '" + alias + "' can't be empty or contain spaces");
            return;
        }
        String owner = owners.putIfAbsent(alias, skill.id());
        if (owner != null) {
            errors.add(path(skill) + ".aliases: '" + alias + "' is already used by skill '" + owner + "'");
        }
    }

    private void checkSetupSlots() {
        Map<Integer, String> owners = new HashMap<>();
        for (SkillDefinition skill : skills.values()) {
            if (skill.setup() == null) {
                continue;
            }
            String owner = owners.putIfAbsent(skill.setup().slot(), skill.id());
            if (owner != null) {
                errors.add(path(skill) + ".setup.slot: slot " + skill.setup().slot()
                        + " is already used by skill '" + owner + "'");
            }
        }
    }

    private void checkPicks() {
        long pickable = skills.values().stream().filter(skill -> skill.setup() != null).count();
        if (setup.picks() > pickable) {
            errors.add("setup.picks: " + setup.picks() + " picks but only " + pickable
                    + " skill(s) have a 'setup' section");
        }
    }

    private void checkBook() {
        Map<String, String> pageOf = new HashMap<>();
        for (SkillBookPage page : book) {
            for (String skill : page.skills()) {
                checkBookEntry(page, skill, pageOf);
            }
        }
    }

    private void checkBookEntry(SkillBookPage page, String skill, Map<String, String> pageOf) {
        String where = "book." + page.id() + ".skills";
        if (!skills.containsKey(skill)) {
            errors.add(where + ": '" + skill + "' is not a skill id");
            return;
        }
        String other = pageOf.putIfAbsent(skill, page.id());
        if (other != null) {
            errors.add(where + ": '" + skill + "' is already on page '" + other + "'");
        }
    }

    private static String path(SkillDefinition skill) {
        return "skills." + skill.id();
    }
}
