package org.skysongdev.skysongstats.character;

// What happened when a character asked to focus a proficiency.
public enum FocusRequest {
    // The focus is now that proficiency, with 0 points.
    FOCUSED,
    // It already was the focus; nothing changed.
    ALREADY_FOCUSED,
    // It is at the top tier, so there is nothing left to train.
    MASTERED,
    // The current focus has points that switching would lose; nothing changed
    // until the player confirms.
    NEEDS_CONFIRMATION
}
