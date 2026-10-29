package org.skysongdev.skysongstats.definition;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

// One stat from stats.yml. bar is null for plain values, setup is null for stats
// that don't show in /setup, and min is null when the stat has no floor.
public record StatDefinition(
        String id,
        StatDisplay display,
        int baseline,
        Integer min,
        BarSettings bar,
        List<ItemTagSource> itemTags,
        Map<StatAction, List<String>> commands,
        StatSetup setup) {

    // Bars expose their max under this prefix: bar "hp" has max "max_hp".
    public static final String MAX_PREFIX = "max_";

    public StatDefinition {
        itemTags = List.copyOf(itemTags);
        Map<StatAction, List<String>> copy = new EnumMap<>(StatAction.class);
        commands.forEach((action, labels) -> copy.put(action, List.copyOf(labels)));
        commands = Map.copyOf(copy);
    }

    public StatType type() {
        return bar == null ? StatType.VALUE : StatType.BAR;
    }

    public boolean isBar() {
        return bar != null;
    }

    // The id of this bar's max ("max_hp"), or empty for plain values.
    public Optional<String> maxId() {
        return isBar() ? Optional.of(MAX_PREFIX + id) : Optional.empty();
    }

    public OptionalInt minimum() {
        return min == null ? OptionalInt.empty() : OptionalInt.of(min);
    }

    public Optional<StatSetup> setupOptions() {
        return Optional.ofNullable(setup);
    }

    public List<String> commandsFor(StatAction action) {
        return commands.getOrDefault(action, List.of());
    }
}
