package org.skysongdev.skysongstats.definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// The stats loaded from stats.yml, in file order, with lookups by id, alias and
// command name. Immutable: a reload builds a new one.
public final class StatDefinitions {
    private final Map<String, StatDefinition> byId = new LinkedHashMap<>();
    private final Map<String, StatDefinition> byName = new LinkedHashMap<>();
    private final Map<String, CommandBinding> byCommand = new LinkedHashMap<>();

    // A command name from a stat's "commands:" block and what it runs.
    public record CommandBinding(String label, StatDefinition stat, StatAction action) {
    }

    // The loader has already checked that ids, aliases and command names are unique.
    StatDefinitions(Collection<StatDefinition> stats) {
        for (StatDefinition stat : stats) {
            byId.put(stat.id(), stat);
            byName.put(stat.id(), stat);
            stat.display().aliases().forEach(alias -> byName.put(alias, stat));
            stat.commands().forEach((action, labels) -> labels.forEach(
                    label -> byCommand.put(label, new CommandBinding(label, stat, action))));
        }
    }

    public List<StatDefinition> all() {
        return List.copyOf(byId.values());
    }

    public Optional<StatDefinition> get(String id) {
        return Optional.ofNullable(byId.get(normalize(id)));
    }

    // Finds a stat by its id or one of its aliases, ignoring case ("STR", "strength").
    public Optional<StatDefinition> find(String idOrAlias) {
        return Optional.ofNullable(byName.get(normalize(idOrAlias)));
    }

    // Finds the bar whose max is named, e.g. "max_hp" gives the hp bar.
    public Optional<StatDefinition> findBarByMaxId(String maxId) {
        String normalized = normalize(maxId);
        if (!normalized.startsWith(StatDefinition.MAX_PREFIX)) {
            return Optional.empty();
        }
        return get(normalized.substring(StatDefinition.MAX_PREFIX.length())).filter(StatDefinition::isBar);
    }

    public Optional<CommandBinding> findCommand(String label) {
        return Optional.ofNullable(byCommand.get(normalize(label)));
    }

    public List<CommandBinding> commands() {
        return List.copyOf(byCommand.values());
    }

    public List<StatDefinition> setupStats() {
        return byId.values().stream().filter(stat -> stat.setup() != null).toList();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
