package org.skysongdev.skysongstats.api;

/** What happened when a character was asked to focus a proficiency. */
public enum FocusResult {
    /** The focus is now that proficiency, with 0 points. */
    FOCUSED,
    /** It already was the focus; nothing changed. */
    ALREADY_FOCUSED,
    /** It is at the top tier, so there is nothing left to train; nothing changed. */
    MASTERED,
    /**
     * The current focus has points that switching would lose, and they weren't to be
     * discarded; nothing changed. Ask the player, then call again with {@code discardPoints}.
     */
    NEEDS_CONFIRMATION
}
