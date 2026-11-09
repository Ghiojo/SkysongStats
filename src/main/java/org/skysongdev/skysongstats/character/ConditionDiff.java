package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.ConditionDefinitions;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Compares a character's conditions before and after a change.
final class ConditionDiff {
    private ConditionDiff() {
    }

    // Every condition that started, stopped or changed, in conditions.yml order;
    // conditions gone from the file come last.
    static List<ConditionChange> between(ProfileKey character, List<ActiveCondition> before,
                                         List<ActiveCondition> after, ConditionDefinitions definitions) {
        Map<String, ActiveCondition> was = byId(before);
        Map<String, ActiveCondition> is = byId(after);
        Set<String> ids = new LinkedHashSet<>();
        definitions.all().stream().map(ConditionDefinition::id).forEach(ids::add);
        ids.addAll(was.keySet());
        List<ConditionChange> changes = new ArrayList<>();
        for (String id : ids) {
            ActiveCondition from = was.get(id);
            ActiveCondition to = is.get(id);
            if (!Objects.equals(state(from), state(to))) {
                changes.add(new ConditionChange(character, Optional.ofNullable(from), Optional.ofNullable(to)));
            }
        }
        return changes;
    }

    // Its level and sources, null when the character doesn't have it. Comparing
    // these, a reload that only re-words a condition's name or description
    // changes nothing on the character.
    private static List<Object> state(ActiveCondition condition) {
        return condition == null ? null
                : List.of(condition.level(), condition.direct(), condition.fromInjuries(), condition.fromConditions());
    }

    private static Map<String, ActiveCondition> byId(List<ActiveCondition> conditions) {
        return conditions.stream().collect(Collectors.toMap(ActiveCondition::id, Function.identity()));
    }
}
