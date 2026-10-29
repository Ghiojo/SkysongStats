package org.skysongdev.skysongstats.definition;

import java.util.List;

// How a stat shows in the /setup point allocation screen. min and max are how far
// a player can move the stat from its baseline (today: -3 to +4).
public record StatSetup(Icon icon, int slot, int min, int max, List<String> lore) {
    // Slots of the /setup screen that hold its own items, so no stat can use them.
    public static final int HEADER_SLOT = 4;
    public static final int CONFIRM_SLOT = 22;

    public StatSetup {
        lore = List.copyOf(lore);
    }
}
