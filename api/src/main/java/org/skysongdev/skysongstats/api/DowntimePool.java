package org.skysongdev.skysongstats.api;

/** The two downtime pools. */
public enum DowntimePool {
    /** One per player, shared by all their characters. */
    GLOBAL,
    /** One per character (profile). */
    CHARACTER
}
