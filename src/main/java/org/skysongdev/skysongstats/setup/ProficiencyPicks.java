package org.skysongdev.skysongstats.setup;

import org.skysongdev.skysongstats.definition.ProficiencyDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// The proficiencies a player picks on the /setup proficiency screen. Each skill
// has its own number of picks, and a pick only spends picks of the proficiency's
// skill. A proficiency is picked at most once, and the trained ones (which every
// new character already gets) can't be picked at all.
public final class ProficiencyPicks {
    private final Map<String, Integer> allowed;
    private final Set<String> trained;
    private final List<ProficiencyDefinition> picked = new ArrayList<>();

    // allowed holds the picks per skill id, trained the ids of the trained proficiencies.
    public ProficiencyPicks(Map<String, Integer> allowed, Set<String> trained) {
        this.allowed = new HashMap<>(allowed);
        this.trained = Set.copyOf(trained);
    }

    // Returns false, changing nothing, when it's trained, already picked, or its
    // skill has no picks left.
    public boolean pick(ProficiencyDefinition proficiency) {
        if (isTrained(proficiency) || isPicked(proficiency) || left(proficiency.skill()) <= 0) {
            return false;
        }
        picked.add(proficiency);
        return true;
    }

    // Returns false when it wasn't picked.
    public boolean unpick(ProficiencyDefinition proficiency) {
        return picked.removeIf(other -> other.id().equals(proficiency.id()));
    }

    public boolean isPicked(ProficiencyDefinition proficiency) {
        return picked.stream().anyMatch(other -> other.id().equals(proficiency.id()));
    }

    public boolean isTrained(ProficiencyDefinition proficiency) {
        return trained.contains(proficiency.id());
    }

    public void reset() {
        picked.clear();
    }

    // The picks of this skill not used yet.
    public int left(String skill) {
        long used = picked.stream().filter(proficiency -> proficiency.skill().equals(skill)).count();
        return allowed.getOrDefault(skill, 0) - (int) used;
    }

    // The skill ids that have picks, in no particular order.
    public Set<String> skills() {
        return Set.copyOf(allowed.keySet());
    }

    // A skill that still has picks and something left to pick among shown, or
    // empty when the player may confirm. A skill with nothing left to pick doesn't
    // hold the player back.
    public Optional<String> skillStillToPick(List<ProficiencyDefinition> shown) {
        return shown.stream()
                .filter(proficiency -> !isTrained(proficiency) && !isPicked(proficiency))
                .map(ProficiencyDefinition::skill)
                .filter(skill -> left(skill) > 0)
                .findFirst();
    }

    // The picked proficiencies, in the order they were picked.
    public List<ProficiencyDefinition> picked() {
        return List.copyOf(picked);
    }
}
