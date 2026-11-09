package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.api.EffectSource;

import java.util.Objects;

// A stat modifier a character has because of a current injury or condition, from
// its "effects: modifiers:". Worked out from what the character has, never saved.
// target is a stat id or max_<bar>; amount is already multiplied by the
// condition's level; source and sourceId say which injury or condition gives it,
// sourceName is its name (MiniMessage, from injuries.yml or conditions.yml).
public record StatEffect(String target, int amount, EffectSource source, String sourceId, String sourceName) {
    public StatEffect {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(sourceName, "sourceName");
    }
}
