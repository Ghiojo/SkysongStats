package org.skysongdev.skysongstats.definition;

import java.util.Objects;

// One stat modifier an injury or condition gives while a character has it, read
// from "effects: modifiers:" ("str-1", "max_hp-5", "con-$"). target is a stat id
// or max_<bar>, already resolved from any alias. A per-level modifier (written
// with $) gives amount times the condition's level; amount is then +1 or -1.
public record EffectModifier(String target, int amount, boolean perLevel) {
    public EffectModifier {
        Objects.requireNonNull(target, "target");
    }

    // What this modifier adds for a condition at this level. Injuries have no
    // level, so they always pass 1.
    public int amountAt(int level) {
        return perLevel ? amount * level : amount;
    }
}
