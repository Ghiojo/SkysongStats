package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Optional;

// The levels from skills.yml, lowest first. The loader has checked that the first
// starts at 0 XP and that each needs more XP than the one before.
public record SkillLevels(List<SkillLevel> all) {
    public SkillLevels {
        all = List.copyOf(all);
    }

    // The level reached with this much XP. Below 0 XP it is the first level.
    public SkillLevel levelFor(int xp) {
        SkillLevel reached = all.getFirst();
        for (SkillLevel level : all) {
            if (xp >= level.xp()) {
                reached = level;
            }
        }
        return reached;
    }

    // The levels up to and including the one reached with this much XP, lowest first.
    public List<SkillLevel> reachedWith(int xp) {
        return all.subList(0, all.indexOf(levelFor(xp)) + 1);
    }

    public Optional<SkillLevel> get(String id) {
        return all.stream().filter(level -> level.id().equals(id)).findFirst();
    }
}
