package org.skysongdev.skysongstats.definition;

import java.util.List;

// How a proficiency shows on the /setup proficiency screen: its icon (a book
// unless proficiencies.yml names one) and the lore lines under its name.
public record ProficiencySetup(Icon icon, List<String> lore) {
    public static final String DEFAULT_ICON = "BOOK";

    public ProficiencySetup {
        lore = List.copyOf(lore);
    }
}
