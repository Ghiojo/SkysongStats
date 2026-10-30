package org.skysongdev.skysongstats.skill;

import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillLevel;
import org.skysongdev.skysongstats.definition.SkillLevels;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// The LuckPerms nodes of a skill's levels: skysongstats.skill.<skill>.<level>.
// Every level except the first has one, and a character has the node of its
// level and of every level below it.
public final class SkillPermissions {
    public static final String PREFIX = "skysongstats.skill.";

    private SkillPermissions() {
    }

    public static String node(SkillDefinition skill, SkillLevel level) {
        return PREFIX + skill.id() + "." + level.id();
    }

    // Every node the skill's levels give, lowest level first.
    public static Set<String> all(SkillDefinition skill, SkillLevels levels) {
        return nodes(skill, levels.all());
    }

    // The nodes a character with this much XP in the skill has.
    public static Set<String> reached(SkillDefinition skill, SkillLevels levels, int xp) {
        return nodes(skill, levels.reachedWith(xp));
    }

    private static Set<String> nodes(SkillDefinition skill, List<SkillLevel> levels) {
        Set<String> nodes = new LinkedHashSet<>();
        levels.stream().skip(1).forEach(level -> nodes.add(node(skill, level)));
        return nodes;
    }
}
