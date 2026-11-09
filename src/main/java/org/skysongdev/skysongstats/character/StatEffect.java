package org.skysongdev.skysongstats.character;

import java.util.Objects;

// A stat modifier a character has because of a current injury or condition, from
// its "effects: modifiers:". Worked out from what the character has, never saved.
// target is a stat id or max_<bar>; amount is already multiplied by the
// condition's level; source is the name of the injury or condition giving it
// (MiniMessage, from injuries.yml or conditions.yml).
public record StatEffect(String target, int amount, String source) {
    public StatEffect {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
    }
}
