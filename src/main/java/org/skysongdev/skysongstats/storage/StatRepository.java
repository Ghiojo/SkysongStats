package org.skysongdev.skysongstats.storage;

import java.util.Map;

// Where character stats and modifiers are saved. Values and modifiers are keyed
// by stat id, so adding a stat to stats.yml needs no database change.
public interface StatRepository {

    // Creates the tables if they don't exist yet.
    void createTables();

    Map<ProfileKey, StoredStats> loadAll();

    // Creates or replaces one stat's saved value.
    void saveValue(ProfileKey character, String statId, int value);

    void saveModifier(ProfileKey character, StoredModifier modifier);

    void deleteModifier(String modifierId);

    // Removes every value and modifier of a character, for deleted profiles.
    void deleteCharacter(ProfileKey character);
}
