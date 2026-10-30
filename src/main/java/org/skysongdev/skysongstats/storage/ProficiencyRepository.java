package org.skysongdev.skysongstats.storage;

import java.util.Map;

// Where character proficiencies are saved: one row per proficiency with its tier
// id, and at most one focus row per character. Proficiencies and tiers are saved
// by id, so changing proficiencies.yml needs no database change.
public interface ProficiencyRepository {

    // Creates the tables if they don't exist yet.
    void createTables();

    // Every character with a saved tier or focus.
    Map<ProfileKey, StoredProficiencies> loadAll();

    // Creates or replaces one proficiency's saved tier.
    void saveTier(ProfileKey character, String proficiencyId, String tierId);

    // Creates or replaces the character's focus and its points.
    void saveFocus(ProfileKey character, StoredFocus focus);

    // Removes the character's focus, after it reaches the top tier.
    void clearFocus(ProfileKey character);

    // Removes all of a character's tiers and its focus, for deleted profiles.
    void deleteCharacter(ProfileKey character);
}
