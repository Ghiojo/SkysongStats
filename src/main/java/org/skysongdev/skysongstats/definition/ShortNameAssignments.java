package org.skysongdev.skysongstats.definition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// Short names that plugins assigned to stats at runtime, over stats.yml's. They are
// kept in memory only, in the order they were made. For each stat the newest
// assignment that fits is the one in effect; when it goes, the one before it (or
// stats.yml) comes back. They outlive a reload: apply() is called
// again with the new stats.yml. O is whoever made them (the plugin).
public final class ShortNameAssignments<O> {

    // One assignment. A removed one stays removed.
    public static final class Assignment<O> {
        private final O owner;
        private final String statId;
        private final String shortName;
        private boolean removed;

        private Assignment(O owner, String statId, String shortName) {
            this.owner = owner;
            this.statId = statId;
            this.shortName = shortName;
        }

        public O owner() {
            return owner;
        }

        public String statId() {
            return statId;
        }

        public String shortName() {
            return shortName;
        }

        public boolean isRemoved() {
            return removed;
        }
    }

    private final List<Assignment<O>> assignments = new ArrayList<>();

    // Adds an assignment and returns it, or empty (and changes nothing) when the
    // stat isn't in stats.yml, the short name is blank, or commands would read it
    // as another stat's id, alias or short name.
    public Optional<Assignment<O>> assign(StatDefinitions config, O owner, String statId, String shortName) {
        Objects.requireNonNull(owner, "owner");
        Optional<StatDefinition> stat = config.get(statId);
        String trimmed = shortName == null ? "" : shortName.trim();
        if (stat.isEmpty() || trimmed.isEmpty()) {
            return Optional.empty();
        }
        Assignment<O> assignment = new Assignment<>(owner, stat.get().id(), trimmed);
        assignments.add(assignment);
        if (winners(config).get(assignment.statId) != assignment) {
            assignments.remove(assignment);
            return Optional.empty();
        }
        return Optional.of(assignment);
    }

    // Removes one assignment. False when it was already removed.
    public boolean remove(Assignment<O> assignment) {
        if (!assignments.remove(assignment)) {
            return false;
        }
        assignment.removed = true;
        return true;
    }

    // Removes every assignment of an owner. False when it had none.
    public boolean removeOwner(O owner) {
        List<Assignment<O>> owned = assignments.stream().filter(assignment -> assignment.owner.equals(owner)).toList();
        owned.forEach(this::remove);
        return !owned.isEmpty();
    }

    // Every assignment not removed, oldest first, including ones not in effect.
    public List<Assignment<O>> all() {
        return List.copyOf(assignments);
    }

    // Whether this assignment is the one a stat shows now.
    public boolean isActive(StatDefinitions config, Assignment<O> assignment) {
        return winners(config).get(assignment.statId) == assignment;
    }

    // The stats with the short names in effect.
    public StatDefinitions apply(StatDefinitions config) {
        Map<String, String> shortNames = new HashMap<>();
        winners(config).forEach((statId, assignment) -> shortNames.put(statId, assignment.shortName));
        return shortNames.isEmpty() ? config : config.withShortNames(shortNames);
    }

    // For each stat, the assignment in effect. They are played oldest first: one
    // replaces its stat's earlier winner when commands wouldn't read it as another
    // stat (its id, alias, short name from stats.yml or winning assignment), so a
    // newer one never takes a name another stat already shows.
    private Map<String, Assignment<O>> winners(StatDefinitions config) {
        Map<String, String> fixed = takenNames(config);
        Map<String, String> shown = new HashMap<>();
        Map<String, Assignment<O>> winners = new LinkedHashMap<>();
        for (Assignment<O> assignment : assignments) {
            if (config.get(assignment.statId).isEmpty()) {
                continue;
            }
            Optional<String> input = StatDisplay.inputOf(assignment.shortName);
            String owner = input.map(word -> fixed.getOrDefault(word, shown.get(word))).orElse(null);
            if (owner == null || owner.equals(assignment.statId)) {
                Assignment<O> previous = winners.put(assignment.statId, assignment);
                if (previous != null) {
                    StatDisplay.inputOf(previous.shortName).ifPresent(word -> shown.remove(word, previous.statId));
                }
                input.ifPresent(word -> shown.put(word, assignment.statId));
            }
        }
        return winners;
    }

    private static Map<String, String> takenNames(StatDefinitions config) {
        Map<String, String> taken = new HashMap<>();
        for (StatDefinition stat : config.all()) {
            taken.put(stat.id(), stat.id());
            stat.display().aliases().forEach(alias -> taken.put(alias, stat.id()));
            stat.display().shortNameInput().ifPresent(input -> taken.put(input, stat.id()));
        }
        return taken;
    }
}
