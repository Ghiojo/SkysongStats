package org.skysongdev.skysongstats.definition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

// The proficiencies.yml checks that look at more than one entry or at skills.yml:
// skills that don't exist, aliases used twice, ids that clash with the
// /proficiency subcommands, and a setup: section naming unknown skills or
// proficiencies or offering more than the /setup screen can show. Adds each
// problem to errors.
final class ProficienciesValidator {
    // Aliases are typed as command arguments, so anything without spaces.
    private static final Pattern ALIAS = Pattern.compile("\\S+");
    // "/proficiency focus confirm" and the other subcommands read these words
    // where a proficiency id can go.
    private static final Set<String> SUBCOMMANDS = Set.of("view", "focus", "points", "tier", "confirm");

    private final Map<String, ProficiencyDefinition> proficiencies;
    private final SkillDefinitions skills;
    private final List<String> errors;

    ProficienciesValidator(Map<String, ProficiencyDefinition> proficiencies, SkillDefinitions skills,
                           List<String> errors) {
        this.proficiencies = proficiencies;
        this.skills = skills;
        this.errors = errors;
    }

    void validate(ProficiencySetupRules setup) {
        checkSkills();
        checkSubcommandNames();
        checkAliases();
        checkSetupSkills("setup.picks", setup.picks());
        checkSetupSkills("setup.skill-picks", setup.skillPicks());
        checkTrained(setup.trained());
        checkScreenFits(setup);
    }

    private void checkSetupSkills(String path, Map<String, Integer> picks) {
        for (String skill : picks.keySet()) {
            if (skills.get(skill).isEmpty()) {
                errors.add(path + "." + skill + ": '" + skill + "' is not a skill id in skills.yml");
            }
        }
    }

    private void checkTrained(List<String> trained) {
        for (String id : trained) {
            if (!proficiencies.containsKey(id)) {
                errors.add("setup.trained: '" + id + "' is not a proficiency id");
            }
        }
    }

    // The screen shows the trained proficiencies and every proficiency of a skill
    // that can get picks, one per slot.
    private void checkScreenFits(ProficiencySetupRules setup) {
        long shown = proficiencies.values().stream()
                .filter(proficiency -> setup.trained().contains(proficiency.id())
                        || setup.picks().getOrDefault(proficiency.skill(), 0) > 0
                        || setup.skillPicks().getOrDefault(proficiency.skill(), 0) > 0)
                .count();
        if (shown > ProficiencySetupRules.SCREEN_SLOTS) {
            errors.add("setup: the /setup screen can show " + ProficiencySetupRules.SCREEN_SLOTS
                    + " proficiencies, but the trained ones and the skills with picks have " + shown);
        }
    }

    private void checkSkills() {
        for (ProficiencyDefinition proficiency : proficiencies.values()) {
            if (!proficiency.skill().isEmpty() && skills.get(proficiency.skill()).isEmpty()) {
                errors.add(path(proficiency) + ".skill: '" + proficiency.skill() + "' is not a skill id in skills.yml");
            }
        }
    }

    private void checkSubcommandNames() {
        for (ProficiencyDefinition proficiency : proficiencies.values()) {
            if (SUBCOMMANDS.contains(proficiency.id())) {
                errors.add(path(proficiency) + ": '" + proficiency.id() + "' is a /proficiency subcommand, pick another id");
            }
        }
    }

    private void checkAliases() {
        Map<String, String> owners = new HashMap<>();
        proficiencies.keySet().forEach(id -> owners.put(id, id));
        for (ProficiencyDefinition proficiency : proficiencies.values()) {
            for (String alias : proficiency.aliases()) {
                checkAlias(proficiency, alias, owners);
            }
        }
    }

    private void checkAlias(ProficiencyDefinition proficiency, String alias, Map<String, String> owners) {
        if (!ALIAS.matcher(alias).matches()) {
            errors.add(path(proficiency) + ".aliases: '" + alias + "' can't be empty or contain spaces");
            return;
        }
        if (SUBCOMMANDS.contains(alias)) {
            errors.add(path(proficiency) + ".aliases: '" + alias + "' is a /proficiency subcommand");
            return;
        }
        String owner = owners.putIfAbsent(alias, proficiency.id());
        if (owner != null) {
            errors.add(path(proficiency) + ".aliases: '" + alias + "' is already used by proficiency '" + owner + "'");
        }
    }

    private static String path(ProficiencyDefinition proficiency) {
        return "proficiencies." + proficiency.id();
    }
}
