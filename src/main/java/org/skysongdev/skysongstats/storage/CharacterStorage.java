package org.skysongdev.skysongstats.storage;

// Where the stats, skill XP and proficiencies of every character are saved. Closing waits for
// the saves that are still queued.
public interface CharacterStorage extends AutoCloseable {
    StatRepository repository();

    SkillRepository skillRepository();

    ProficiencyRepository proficiencyRepository();

    @Override
    void close();
}
