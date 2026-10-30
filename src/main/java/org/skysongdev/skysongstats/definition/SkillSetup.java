package org.skysongdev.skysongstats.definition;

import java.util.List;

// How a skill shows on the /setup skill screen. color is empty when the screen
// uses the skill's own colour.
public record SkillSetup(Icon icon, int slot, String color, List<String> lore) {
    // The screen's top and bottom rows hold its border, header and confirm button.
    public static final int FIRST_SLOT = 9;
    public static final int LAST_SLOT = 44;

    public SkillSetup {
        lore = List.copyOf(lore);
    }
}
