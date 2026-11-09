package org.skysongdev.skysongstats.storage;

import java.util.Map;

// Where SkysongStats remembers which characters belong to another plugin (an NPC
// plugin's NPCs) instead of a player's profile. Only the owner is kept here: the
// characters' stats, skills and the rest are in the same tables as players'.
public interface PluginCharacterRepository {

    // Creates the table if it doesn't exist yet.
    void createTables();

    // Every plugin character, with the name of the plugin that owns it.
    Map<ProfileKey, String> loadAll();

    // Saves a new plugin character and its owner.
    void save(ProfileKey character, String owner);

    // Forgets a plugin character, for deleted ones.
    void delete(ProfileKey character);
}
