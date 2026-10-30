package org.skysongdev.skysongstats.storage;

// Where the stats and skill XP of every character are saved. Closing waits for
// the saves that are still queued.
public interface CharacterStorage extends AutoCloseable {
    StatRepository repository();

    SkillRepository skillRepository();

    @Override
    void close();
}
