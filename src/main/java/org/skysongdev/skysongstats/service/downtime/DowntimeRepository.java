package org.skysongdev.skysongstats.service.downtime;

import java.util.Map;
import java.util.UUID;

/**
 * Where downtime is stored. {@link DowntimeService} keeps everything in memory
 * and calls these methods to save each change before it updates its cache.
 *
 * Implementations throw an unchecked exception when a write fails, so the
 * service leaves its cache unchanged.
 */
public interface DowntimeRepository {

    DowntimeSnapshot loadAll();

    /** Creates or updates one player's global downtime. */
    void saveGlobal(UUID player, int value);

    /** Creates or updates one profile's character downtime. */
    void saveCharacter(CharacterKey key, int value);

    void deleteCharacter(CharacterKey key);

    /** Creates or updates a player's spending preference: {@code true} spends from global. */
    void savePreference(UUID player, boolean spendsGlobal);

    /** Saves many values at once, for {@code resetall}. */
    void saveAll(Map<UUID, Integer> globals, Map<CharacterKey, Integer> characters);
}
