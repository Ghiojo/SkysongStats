package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ConditionDefinition;

import java.util.List;
import java.util.Objects;

// A condition a character has now, and why:
//   direct          given with /condition award or the API
//   fromInjuries    ids of the current injuries that give it
//   fromConditions  ids of the current conditions that give it
// level is the highest of all of them. It lasts until every source is gone.
public record ActiveCondition(ConditionDefinition condition, int level, boolean direct,
                              List<String> fromInjuries, List<String> fromConditions) {
    public ActiveCondition {
        Objects.requireNonNull(condition, "condition");
        fromInjuries = List.copyOf(fromInjuries);
        fromConditions = List.copyOf(fromConditions);
    }

    public String id() {
        return condition.id();
    }

    // Whether only an injury or another condition keeps it, so /condition remove
    // can't take it away.
    public boolean onlyGiven() {
        return !direct;
    }
}
