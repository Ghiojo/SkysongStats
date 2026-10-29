package org.skysongdev.skysongstats.storage;

import java.util.Objects;

// A saved modifier. id is permanent (a random UUID made when the modifier is
// created), so deleting one modifier can never hit another. target is a stat id
// or max_<bar>.
public record StoredModifier(String id, String target, int amount) {
    public StoredModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(target, "target");
    }
}
