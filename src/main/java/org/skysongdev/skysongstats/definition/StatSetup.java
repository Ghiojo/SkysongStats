package org.skysongdev.skysongstats.definition;

import java.util.List;

// How a stat shows in the /setup point allocation screen. min and max are how far
// a player can move the stat from its baseline (today: -3 to +4).
public record StatSetup(Icon icon, int slot, int min, int max, List<String> lore) {
    public StatSetup {
        lore = List.copyOf(lore);
    }
}
