package org.skysongdev.skysongstats.definition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Finds conditions that give each other in a loop (bleeding gives weakened,
// weakened gives bleeding), including a condition that gives itself. A loop would
// mean neither could ever go away, so conditions.yml refuses it.
final class ConditionLoops {
    private ConditionLoops() {
    }

    // One text per loop found, like "bleeding -> weakened -> bleeding: conditions
    // can't give each other in a loop". Each loop is reported once.
    static List<String> find(Map<String, ConditionDefinition> conditions) {
        List<String> loops = new ArrayList<>();
        Set<String> done = new HashSet<>();
        for (String id : conditions.keySet()) {
            walk(id, conditions, new ArrayList<>(), done, loops);
        }
        return loops;
    }

    // Depth-first from id along the conditions it gives. path is the chain that
    // led here; meeting a condition already on it closes a loop.
    private static void walk(String id, Map<String, ConditionDefinition> conditions, List<String> path,
                             Set<String> done, List<String> loops) {
        int seen = path.indexOf(id);
        if (seen >= 0) {
            List<String> loop = new ArrayList<>(path.subList(seen, path.size()));
            loop.add(id);
            loops.add(String.join(" -> ", loop) + ": conditions can't give each other in a loop");
            return;
        }
        ConditionDefinition condition = conditions.get(id);
        if (condition == null || done.contains(id)) {
            return;
        }
        path.add(id);
        for (String given : condition.conditions().keySet()) {
            walk(given, conditions, path, done, loops);
        }
        path.removeLast();
        done.add(id);
    }
}
