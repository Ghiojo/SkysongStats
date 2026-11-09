package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Objects;

// An injury given to a character. isNew is false when the character already had
// it (awarding again keeps one, with the new time). learnt tells whether the
// award also taught the character the injury.
public record InjuryAward(ProfileKey character, CharacterInjury injury, boolean isNew, boolean learnt) {
    public InjuryAward {
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(injury, "injury");
    }
}
