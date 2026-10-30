package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

// One skill's XP before and after a change.
public record SkillChange(ProfileKey character, SkillDefinition skill, int before, int after) {
    public boolean changed() {
        return before != after;
    }
}
