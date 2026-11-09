package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.InjuryDefinition;

import java.util.Objects;

// An injury a character has now, awarded at epoch millis.
public record CharacterInjury(InjuryDefinition injury, long awardedAt) {
    public CharacterInjury {
        Objects.requireNonNull(injury, "injury");
    }
}
