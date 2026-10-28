package org.skysongdev.skysongstats.service.downtime;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Downtime limits from the {@code downtime:} section of config.yml.
 * Missing or negative values fall back to the defaults, which are the values
 * the plugin used before they were configurable.
 *
 * @param characterMax   new profiles start here, resetall sets every profile to it, add stops here
 * @param globalStart    new players start here
 * @param globalRecovery resetall adds this to every player's global downtime
 * @param globalMax      resetall and add never go above this
 */
public record DowntimeSettings(int characterMax, int globalStart, int globalRecovery, int globalMax) {

    public static final int DEFAULT_CHARACTER_MAX = 3;
    public static final int DEFAULT_GLOBAL_START = 3;
    public static final int DEFAULT_GLOBAL_RECOVERY = 3;
    public static final int DEFAULT_GLOBAL_MAX = 9;

    public static DowntimeSettings defaults() {
        return new DowntimeSettings(DEFAULT_CHARACTER_MAX, DEFAULT_GLOBAL_START, DEFAULT_GLOBAL_RECOVERY, DEFAULT_GLOBAL_MAX);
    }

    /** Reads the {@code downtime:} section; {@code null} (no section) gives the defaults. */
    public static DowntimeSettings fromConfig(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        return new DowntimeSettings(
                read(section, "character-max", DEFAULT_CHARACTER_MAX),
                read(section, "global-start", DEFAULT_GLOBAL_START),
                read(section, "global-recovery", DEFAULT_GLOBAL_RECOVERY),
                read(section, "global-max", DEFAULT_GLOBAL_MAX));
    }

    private static int read(ConfigurationSection section, String key, int fallback) {
        int value = section.getInt(key, fallback);
        return value < 0 ? fallback : value;
    }

    /** The highest value add can reach in a pool. */
    public int maxFor(DowntimePool pool) {
        return pool == DowntimePool.GLOBAL ? globalMax : characterMax;
    }
}
