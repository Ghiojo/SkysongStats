package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;

// The result of adding training points: what happened, and the focused
// proficiency with its tier and points afterwards. proficiency, tier and points
// are null and 0 when the character has no focus.
public record Training(Outcome outcome, ProficiencyDefinition proficiency, ProficiencyTier tier, int points) {

    public enum Outcome {
        // The points were added and the focus stays at its tier.
        ADDED,
        // The points reached the next tier: the focus went up and its points are back at 0.
        TIER_UP,
        // Nothing added: the character has no focus.
        NO_FOCUS,
        // Nothing added: the focus belongs to another skill than the training.
        OTHER_SKILL,
        // Nothing added: the focus is already at or above the trainer's maximum tier.
        AT_MAX_TIER
    }

    public boolean added() {
        return outcome == Outcome.ADDED || outcome == Outcome.TIER_UP;
    }
}
