package org.skysongdev.skysongstats.storage;

import java.util.Map;

// Where each character's ancestry and the adjustments it gave are saved.
public interface AncestryRepository {

    // Creates the tables if they don't exist yet.
    void createTables();

    // Every character that has an ancestry.
    Map<ProfileKey, StoredAncestry> loadAll();

    // Saves the character's ancestry, replacing what was saved before.
    void save(ProfileKey character, StoredAncestry ancestry);

    // Forgets the character's ancestry: for characters left without one, and for
    // deleted characters.
    void delete(ProfileKey character);
}
