package org.skysongdev.skysongstats.api;

/**
 * What gives a character an {@link Effect}.
 *
 * @since 1.2.0
 */
public enum EffectSource {
    /** One of the character's current injuries. */
    INJURY,
    /** One of the character's current conditions. */
    CONDITION
}
