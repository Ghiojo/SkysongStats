package org.skysongdev.skysongstats.definition;

// The global /setup point rules: how many points a player allocates, and how many
// points they can take away from stats (below the baseline) to get more points.
public record SetupRules(int points, int negativePoints) {
}
