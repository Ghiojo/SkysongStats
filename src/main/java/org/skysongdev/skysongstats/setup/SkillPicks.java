package org.skysongdev.skysongstats.setup;

import org.skysongdev.skysongstats.definition.SkillDefinition;

import java.util.ArrayList;
import java.util.List;

// The skills a player picks on the /setup skill screen: up to the number of
// picks skills.yml allows, each at most once.
public final class SkillPicks {
    private final int allowed;
    private final List<SkillDefinition> picked = new ArrayList<>();

    public SkillPicks(int allowed) {
        this.allowed = allowed;
    }

    // Returns false, changing nothing, when no picks are left or it's already picked.
    public boolean pick(SkillDefinition skill) {
        if (left() <= 0 || isPicked(skill)) {
            return false;
        }
        picked.add(skill);
        return true;
    }

    // Returns false when the skill wasn't picked.
    public boolean unpick(SkillDefinition skill) {
        return picked.removeIf(other -> other.id().equals(skill.id()));
    }

    public boolean isPicked(SkillDefinition skill) {
        return picked.stream().anyMatch(other -> other.id().equals(skill.id()));
    }

    public void reset() {
        picked.clear();
    }

    public int left() {
        return allowed - picked.size();
    }

    // Every pick must be used.
    public boolean canConfirm() {
        return left() == 0;
    }

    // The picked skills, in the order they were picked.
    public List<SkillDefinition> picked() {
        return List.copyOf(picked);
    }
}
