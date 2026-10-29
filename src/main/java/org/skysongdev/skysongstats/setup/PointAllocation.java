package org.skysongdev.skysongstats.setup;

import org.skysongdev.skysongstats.definition.SetupRules;
import org.skysongdev.skysongstats.definition.StatDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// The point rules of /setup, for the stats that have a setup: section in
// stats.yml. Each stat starts at its baseline and moves between baseline + min
// and baseline + max. Raising a stat costs a point. Taking a stat below its
// baseline gives a point back but uses up one of the negative points; raising it
// back towards the baseline returns that negative point.
public final class PointAllocation {
    private final List<StatDefinition> stats;
    private final SetupRules rules;
    private final Map<String, Integer> offsets = new LinkedHashMap<>();
    private int points;
    private int negativePoints;

    public PointAllocation(List<StatDefinition> stats, SetupRules rules) {
        this.stats = List.copyOf(stats);
        this.rules = Objects.requireNonNull(rules, "rules");
        reset();
    }

    // Back to the baselines with every point unspent.
    public void reset() {
        points = rules.points();
        negativePoints = rules.negativePoints();
        stats.forEach(stat -> offsets.put(stat.id(), 0));
    }

    // Raises the stat by 1. Returns false, changing nothing, when no points are
    // left or the stat is at its max.
    public boolean raise(StatDefinition stat) {
        int offset = offset(stat);
        if (points <= 0 || offset + 1 > stat.setup().max()) {
            return false;
        }
        if (offset < 0) {
            negativePoints++;
        }
        offsets.put(stat.id(), offset + 1);
        points--;
        return true;
    }

    // Lowers the stat by 1. Returns false, changing nothing, when the stat is at
    // its min, or it would go below its baseline with no negative points left.
    public boolean lower(StatDefinition stat) {
        int offset = offset(stat);
        if (offset <= stat.setup().min()) {
            return false;
        }
        if (offset <= 0) {
            if (negativePoints <= 0) {
                return false;
            }
            negativePoints--;
        }
        offsets.put(stat.id(), offset - 1);
        points++;
        return true;
    }

    // Kept from the old screen: every point must be spent, and the player can't
    // finish with exactly one negative point left.
    public boolean canConfirm() {
        return points == 0 && negativePoints != 1;
    }

    public int value(StatDefinition stat) {
        return stat.baseline() + offset(stat);
    }

    // The chosen value of every stat, by id, in stats.yml order.
    public Map<String, Integer> values() {
        Map<String, Integer> values = new LinkedHashMap<>();
        stats.forEach(stat -> values.put(stat.id(), value(stat)));
        return values;
    }

    public int points() {
        return points;
    }

    public int negativePoints() {
        return negativePoints;
    }

    private int offset(StatDefinition stat) {
        Integer offset = offsets.get(stat.id());
        if (offset == null) {
            throw new IllegalArgumentException("'" + stat.id() + "' is not a /setup stat");
        }
        return offset;
    }
}
