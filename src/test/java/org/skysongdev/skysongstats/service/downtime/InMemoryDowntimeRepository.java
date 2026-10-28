package org.skysongdev.skysongstats.service.downtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A {@link DowntimeRepository} that keeps everything in maps, for tests. */
public class InMemoryDowntimeRepository implements DowntimeRepository {
    public final Map<UUID, Integer> globals = new HashMap<>();
    public final Map<CharacterKey, Integer> characters = new HashMap<>();
    public final Map<UUID, Boolean> spendsGlobal = new HashMap<>();
    public int writes;
    public boolean failWrites;

    @Override
    public DowntimeSnapshot loadAll() {
        return new DowntimeSnapshot(globals, characters, spendsGlobal);
    }

    @Override
    public void saveGlobal(UUID player, int value) {
        write();
        globals.put(player, value);
    }

    @Override
    public void saveCharacter(CharacterKey key, int value) {
        write();
        characters.put(key, value);
    }

    @Override
    public void deleteCharacter(CharacterKey key) {
        write();
        characters.remove(key);
    }

    @Override
    public void savePreference(UUID player, boolean spendsGlobal) {
        write();
        this.spendsGlobal.put(player, spendsGlobal);
    }

    @Override
    public void saveAll(Map<UUID, Integer> globals, Map<CharacterKey, Integer> characters) {
        write();
        this.globals.putAll(globals);
        this.characters.putAll(characters);
    }

    private void write() {
        if (failWrites) {
            throw new IllegalStateException("database down");
        }
        writes++;
    }
}
