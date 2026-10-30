package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Optional;

// A character's focus before and after a change: a new focus, points added,
// removed or set, or the focus cleared after reaching the top tier. before and
// after are null when there was or is no focus.
public record ProficiencyFocusChange(ProfileKey character, ProficiencyFocus before, ProficiencyFocus after) {

    public Optional<ProficiencyFocus> focusBefore() {
        return Optional.ofNullable(before);
    }

    public Optional<ProficiencyFocus> focusAfter() {
        return Optional.ofNullable(after);
    }
}
