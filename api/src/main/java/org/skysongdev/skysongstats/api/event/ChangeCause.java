package org.skysongdev.skysongstats.api.event;

/** What made a SkysongStats value change, as the change events carry it. */
public enum ChangeCause {
    /** A command, such as {@code /skills addxp} or {@code /heal}. */
    COMMAND,
    /** A player finishing {@code /setup}. */
    SETUP,
    /** Training points added to a proficiency by a trainer. */
    TRAINING,
    /** {@code /ssreload} applying new config, for example a bar brought down to a lower max. */
    RELOAD,
    /** Another plugin, through the SkysongStats API. */
    API
}
