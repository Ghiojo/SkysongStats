package org.skysongdev.skysongstats.storage;

// A character's saved proficiency focus: the proficiency it trains and the
// points it has towards that proficiency's next tier.
public record StoredFocus(String proficiencyId, int points) {
}
