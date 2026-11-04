package org.skysongdev.skysongstats.api;

/**
 * The limits from the {@code downtime:} section of config.yml.
 *
 * @param characterMax   new characters start here, a reset sets every character to it, adding stops here
 * @param globalStart    new players start here
 * @param globalRecovery a reset adds this to every player's global downtime
 * @param globalMax      a reset and adding never go above this
 */
public record DowntimeLimits(int characterMax, int globalStart, int globalRecovery, int globalMax) {
}
