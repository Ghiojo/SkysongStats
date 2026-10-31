package org.skysongdev.skysongstats.character;

// Told about every proficiency tier and focus change, e.g. to update the
// LuckPerms tier nodes and fire the Bukkit events.
public interface ProficiencyChangeListener {
    void onTierChange(ProficiencyTierChange change);

    void onFocusChange(ProficiencyFocusChange change);
}
