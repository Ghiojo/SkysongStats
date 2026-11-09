package org.skysongdev.skysongstats.storage;

import java.util.Map;

// Where characters' injuries, directly given conditions and known injuries are
// saved, keyed by the ids of injuries.yml and conditions.yml, so adding one needs
// no database change.
public interface AfflictionRepository {

    // Creates the tables if they don't exist yet.
    void createTables();

    // Every character that has at least one injury, condition or known injury.
    Map<ProfileKey, StoredAfflictions> loadAll();

    // Creates or replaces an injury.
    void saveInjury(ProfileKey character, String injuryId, long awardedAt);

    void deleteInjury(ProfileKey character, String injuryId);

    // Creates or replaces a directly given condition.
    void saveCondition(ProfileKey character, String conditionId, StoredCondition condition);

    void deleteCondition(ProfileKey character, String conditionId);

    // Creates or replaces a known injury.
    void saveKnownInjury(ProfileKey character, String injuryId, StoredKnownInjury known);

    void deleteKnownInjury(ProfileKey character, String injuryId);

    // Removes everything of a character, for deleted profiles.
    void deleteCharacter(ProfileKey character);
}
