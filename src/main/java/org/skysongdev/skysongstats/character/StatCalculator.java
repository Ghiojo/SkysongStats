package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.definition.ItemTagSource;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.storage.StoredModifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

// Works out a character's numbers from its sheet and the stat definitions:
//   base   saved value, else the baseline (bars: the start value)
//   total  base + modifiers on the stat + Skysong item counts (for a bar: its current value)
//   max    the bar's max formula, read with stat totals, + modifiers on max_<bar>
// A calculator is cheap and is made for one question, so it can guard against a
// formula reading a value that is being computed (a full-at-start bar whose max
// reads its own current value): that value counts as 0 instead of looping.
public final class StatCalculator {
    private final StatDefinitions definitions;
    private final CharacterSheet sheet;
    private final Set<String> computingMax = new HashSet<>();

    public StatCalculator(StatDefinitions definitions, CharacterSheet sheet) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.sheet = Objects.requireNonNull(sheet, "sheet");
    }

    public int base(StatDefinition stat) {
        OptionalInt saved = sheet.savedValue(stat.id());
        if (saved.isPresent()) {
            return saved.getAsInt();
        }
        if (!stat.isBar()) {
            return stat.baseline();
        }
        return stat.bar().startAtMax() ? startingMax(stat) : stat.bar().start();
    }

    // What a full-at-start bar starts at: its max, without its own item counts and
    // modifiers, since total() adds those on top.
    private int startingMax(StatDefinition stat) {
        if (computingMax.contains(stat.id())) {
            return 0;
        }
        return max(stat) - extra(stat);
    }

    public int modifiers(String target) {
        int sum = 0;
        for (StoredModifier modifier : sheet.modifiers()) {
            if (modifier.target().equals(target)) {
                sum += modifier.amount();
            }
        }
        return sum;
    }

    public int items(StatDefinition stat) {
        int sum = 0;
        for (ItemTagSource source : stat.itemTags()) {
            sum += sheet.itemCount(source);
        }
        return sum;
    }

    // Everything on top of the base: modifiers on the stat and item counts.
    public int extra(StatDefinition stat) {
        return modifiers(stat.id()) + items(stat);
    }

    public int total(StatDefinition stat) {
        return base(stat) + extra(stat);
    }

    public int max(StatDefinition bar) {
        if (!bar.isBar()) {
            throw new IllegalArgumentException(bar.id() + " is not a bar");
        }
        if (!computingMax.add(bar.id())) {
            return 0;
        }
        try {
            int formula = bar.bar().max().evaluate(this::formulaValue);
            return formula + modifiers(bar.maxId().orElseThrow());
        } finally {
            computingMax.remove(bar.id());
        }
    }

    private double formulaValue(String reference) {
        return value(reference).orElse(0);
    }

    // The value a placeholder or formula means by a name: a stat id gives its
    // total, max_<bar> gives the bar's max. Empty for unknown names.
    public OptionalInt value(String name) {
        OptionalInt max = definitions.findBarByMaxId(name).map(bar -> OptionalInt.of(max(bar))).orElse(OptionalInt.empty());
        if (max.isPresent()) {
            return max;
        }
        return definitions.get(name).map(stat -> OptionalInt.of(total(stat))).orElse(OptionalInt.empty());
    }
}
