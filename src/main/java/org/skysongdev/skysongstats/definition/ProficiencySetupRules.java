package org.skysongdev.skysongstats.definition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The setup: section of proficiencies.yml, what a new character gets in /setup:
//   trained      proficiency ids every new character starts at the first paid tier in
//   picks        free picks per skill id that every new character gets
//   skillPicks   extra picks of a skill's proficiencies when the player picked
//                that skill on the /setup skill screen
// A pick raises one proficiency from the first tier to the next one, never higher.
public record ProficiencySetupRules(List<String> trained, Map<String, Integer> picks,
                                   Map<String, Integer> skillPicks) {
    // The screen's slots between its top and bottom rows: the most proficiencies it
    // can show.
    public static final int SCREEN_SLOTS = SkillSetup.LAST_SLOT - SkillSetup.FIRST_SLOT + 1;

    public ProficiencySetupRules {
        trained = List.copyOf(trained);
        picks = Map.copyOf(picks);
        skillPicks = Map.copyOf(skillPicks);
    }

    public static ProficiencySetupRules none() {
        return new ProficiencySetupRules(List.of(), Map.of(), Map.of());
    }

    // The picks per skill id for a player who picked these skills on the skill
    // screen. Skills without picks are left out.
    public Map<String, Integer> picksFor(List<String> pickedSkills) {
        Map<String, Integer> total = new HashMap<>();
        picks.forEach((skill, count) -> total.merge(skill, count, Integer::sum));
        pickedSkills.forEach(skill -> total.merge(skill, skillPicks.getOrDefault(skill, 0), Integer::sum));
        total.values().removeIf(count -> count <= 0);
        return total;
    }
}
