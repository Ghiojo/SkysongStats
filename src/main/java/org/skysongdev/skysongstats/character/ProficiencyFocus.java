package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ProficiencyDefinition;

// The proficiency a character is training and its points towards the next tier.
public record ProficiencyFocus(ProficiencyDefinition proficiency, int points) {
}
