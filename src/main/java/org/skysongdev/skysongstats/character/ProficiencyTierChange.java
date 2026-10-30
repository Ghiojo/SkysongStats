package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.storage.ProfileKey;

// One proficiency's tier before and after a change. tierUp is true when it went
// up (training, /setup or tier set), false when tier set lowered it.
public record ProficiencyTierChange(ProfileKey character, ProficiencyDefinition proficiency, ProficiencyTier before,
                                    ProficiencyTier after, boolean tierUp) {
}
