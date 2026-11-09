package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.ConditionDefinitions;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.storage.StoredCondition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Works out a character's current conditions: the ones given directly, the ones
// its injuries give, and the ones those conditions give in turn, as deep as the
// chain goes. conditions.yml refuses loops, and each condition gives its
// conditions once, so this always ends.
final class ConditionResolver {
    private final ConditionDefinitions definitions;

    // Where a condition comes from while it is being worked out.
    private record Source(String conditionId, int level, boolean direct, String injury, String condition) {
    }

    private static final class Building {
        private int level;
        private boolean direct;
        private final List<String> fromInjuries = new ArrayList<>();
        private final List<String> fromConditions = new ArrayList<>();
    }

    ConditionResolver(ConditionDefinitions definitions) {
        this.definitions = definitions;
    }

    // In conditions.yml order. Ids no longer in conditions.yml are left out.
    List<ActiveCondition> resolve(Map<String, StoredCondition> direct, Collection<InjuryDefinition> injuries) {
        Deque<Source> pending = new ArrayDeque<>();
        direct.forEach((id, stored) -> pending.add(new Source(id, stored.level(), true, null, null)));
        for (InjuryDefinition injury : injuries) {
            injury.conditions().forEach((id, level) -> pending.add(new Source(id, level, false, injury.id(), null)));
        }
        Map<String, Building> current = new HashMap<>();
        while (!pending.isEmpty()) {
            Source source = pending.poll();
            definitions.get(source.conditionId()).ifPresent(condition -> add(condition, source, current, pending));
        }
        List<ActiveCondition> active = new ArrayList<>();
        for (ConditionDefinition condition : definitions.all()) {
            Optional.ofNullable(current.get(condition.id())).ifPresent(building -> active.add(new ActiveCondition(
                    condition, building.level, building.direct, building.fromInjuries, building.fromConditions)));
        }
        return active;
    }

    // The first time a condition becomes current, the conditions it gives join in.
    private static void add(ConditionDefinition condition, Source source, Map<String, Building> current,
                            Deque<Source> pending) {
        Building building = current.get(condition.id());
        if (building == null) {
            building = new Building();
            current.put(condition.id(), building);
            condition.conditions().forEach((id, level) -> pending.add(new Source(id, level, false, null, condition.id())));
        }
        building.level = Math.max(building.level, source.level());
        building.direct |= source.direct();
        if (source.injury() != null) {
            building.fromInjuries.add(source.injury());
        }
        if (source.condition() != null) {
            building.fromConditions.add(source.condition());
        }
    }
}
