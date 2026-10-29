package org.skysongdev.skysongstats.definition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The stats.yml checks that look at more than one stat: names and commands used
// twice, formulas reading stats that don't exist, bar maximums that depend on
// each other in a circle, shields, and /setup slots. Adds each problem to errors.
final class StatsValidator {
    // Commands the plugin registers itself, which a stat can't take over.
    static final Set<String> RESERVED_COMMANDS = Set.of(
            "stats", "sstats", "statsreload", "ssreload", "skills", "setup", "modifier",
            "profile", "char", "schar", "downtime", "dt", "proficiency");

    private final Map<String, StatDefinition> stats;
    private final List<String> errors;

    StatsValidator(Map<String, StatDefinition> stats, List<String> errors) {
        this.stats = stats;
        this.errors = errors;
    }

    void validate() {
        checkNames();
        checkCommands();
        checkSetupSlots();
        for (StatDefinition stat : stats.values()) {
            if (stat.isBar()) {
                checkFormula(stat);
                checkShield(stat);
            } else if (!stat.commandsFor(StatAction.DAMAGE).isEmpty()) {
                errors.add(path(stat) + ".commands.damage: only bars with a 'shield' have damage");
            }
        }
        checkCircularMaximums();
    }

    private void checkNames() {
        Map<String, String> owners = new HashMap<>();
        stats.keySet().forEach(id -> owners.put(id, id));
        for (StatDefinition stat : stats.values()) {
            for (String alias : stat.display().aliases()) {
                checkAlias(stat, alias, owners);
            }
        }
    }

    private void checkAlias(StatDefinition stat, String alias, Map<String, String> owners) {
        if (!StatsConfigLoader.ID.matcher(alias).matches() || alias.startsWith(StatDefinition.MAX_PREFIX)) {
            errors.add(path(stat) + ".aliases: '" + alias
                    + "' must use lowercase letters, digits, '_' and '-' and not start with 'max_'");
            return;
        }
        String owner = owners.putIfAbsent(alias, stat.id());
        if (owner != null) {
            errors.add(path(stat) + ".aliases: '" + alias + "' is already used by stat '" + owner + "'");
        }
    }

    private void checkCommands() {
        Map<String, String> owners = new HashMap<>();
        for (StatDefinition stat : stats.values()) {
            stat.commands().values().forEach(labels -> labels.forEach(label -> {
                String owner = owners.putIfAbsent(label, stat.id());
                if (RESERVED_COMMANDS.contains(label)) {
                    errors.add(path(stat) + ".commands: '/" + label + "' is one of the plugin's own commands");
                } else if (owner != null) {
                    errors.add(path(stat) + ".commands: '/" + label + "' is already used by stat '" + owner + "'");
                }
            }));
        }
    }

    private void checkSetupSlots() {
        Map<Integer, String> owners = new HashMap<>();
        for (StatDefinition stat : stats.values()) {
            if (stat.setup() == null) {
                continue;
            }
            String owner = owners.putIfAbsent(stat.setup().slot(), stat.id());
            if (owner != null) {
                errors.add(path(stat) + ".setup.slot: slot " + stat.setup().slot() + " is already used by stat '" + owner + "'");
            }
        }
    }

    private void checkFormula(StatDefinition stat) {
        for (String reference : stat.bar().max().references()) {
            if (!exists(reference)) {
                errors.add(path(stat) + ".max: {" + reference + "} is not a stat id"
                        + (reference.startsWith(StatDefinition.MAX_PREFIX) ? " (only bars have a max_ id)" : ""));
            }
        }
    }

    private boolean exists(String reference) {
        if (reference.startsWith(StatDefinition.MAX_PREFIX)) {
            StatDefinition bar = stats.get(reference.substring(StatDefinition.MAX_PREFIX.length()));
            return bar != null && bar.isBar();
        }
        return stats.containsKey(reference);
    }

    private void checkShield(StatDefinition stat) {
        String shield = stat.bar().shield();
        if (shield == null) {
            if (!stat.commandsFor(StatAction.DAMAGE).isEmpty()) {
                errors.add(path(stat) + ".commands.damage: damage needs a 'shield' stat; use reduce instead");
            }
        } else if (shield.equals(stat.id())) {
            errors.add(path(stat) + ".shield: a stat can't be its own shield");
        } else if (!stats.containsKey(shield)) {
            errors.add(path(stat) + ".shield: '" + shield + "' is not a stat id");
        }
    }

    // A bar's max can read other bars' maximums ({max_hp}), so "max_a reads max_b
    // reads max_a" would never finish. Plain stats and current values are stored,
    // not computed, so only max_ references can form a circle.
    private void checkCircularMaximums() {
        Set<String> done = new HashSet<>();
        for (StatDefinition stat : stats.values()) {
            if (stat.isBar()) {
                visit(stat.id(), new ArrayList<>(), done);
            }
        }
    }

    private void visit(String barId, List<String> chain, Set<String> done) {
        if (chain.contains(barId)) {
            List<String> circle = new ArrayList<>(chain.subList(chain.indexOf(barId), chain.size()));
            circle.add(barId);
            errors.add("stats." + circle.getFirst() + ".max: circular formula, "
                    + String.join(" -> ", circle.stream().map(id -> StatDefinition.MAX_PREFIX + id).toList()));
            return;
        }
        if (!done.add(barId)) {
            return;
        }
        chain.add(barId);
        for (String reference : stats.get(barId).bar().max().references()) {
            if (reference.startsWith(StatDefinition.MAX_PREFIX) && exists(reference)) {
                visit(reference.substring(StatDefinition.MAX_PREFIX.length()), chain, done);
            }
        }
        chain.removeLast();
    }

    private static String path(StatDefinition stat) {
        return "stats." + stat.id();
    }
}
