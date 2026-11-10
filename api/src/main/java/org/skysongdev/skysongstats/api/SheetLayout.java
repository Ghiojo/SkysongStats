package org.skysongdev.skysongstats.api;

/**
 * The sheets in SkysongStats' messages.yml that {@link CharacterSheets#render} can show
 * for any character, with the server's layout.
 *
 * @since 1.3.0
 */
public enum SheetLayout {
    /** The stat sheet {@code /stats view} shows: {@code stats.sheet} and the lines after it. */
    STATS,
    /** The character sheet {@code /character view} shows: {@code character.view.*}. */
    CHARACTER,
    /** The proficiency sheet {@code /proficiency view} shows: {@code proficiency.sheet}. */
    PROFICIENCIES
}
