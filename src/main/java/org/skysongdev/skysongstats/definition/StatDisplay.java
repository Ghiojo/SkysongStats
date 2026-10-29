package org.skysongdev.skysongstats.definition;

import java.util.List;

// How a stat is named: display name, MiniMessage colour, and the other names
// commands accept for it (lowercase).
public record StatDisplay(String name, String color, List<String> aliases) {
    public StatDisplay {
        aliases = List.copyOf(aliases);
    }
}
