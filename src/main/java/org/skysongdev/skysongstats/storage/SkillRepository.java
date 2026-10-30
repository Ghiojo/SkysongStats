package org.skysongdev.skysongstats.storage;

import java.util.Map;

// Where character skill XP is saved, keyed by skill id, so adding a skill to
// skills.yml needs no database change.
public interface SkillRepository {

    // Creates the table if it doesn't exist yet.
    void createTables();

    // Every character's XP by skill id.
    Map<ProfileKey, Map<String, Integer>> loadAll();

    // Creates or replaces one skill's saved XP.
    void saveXp(ProfileKey character, String skillId, int xp);

    // Removes all of a character's XP, for deleted profiles.
    void deleteCharacter(ProfileKey character);
}
