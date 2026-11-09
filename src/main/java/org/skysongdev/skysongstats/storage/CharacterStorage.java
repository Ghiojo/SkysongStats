package org.skysongdev.skysongstats.storage;

// Where the stats, skill XP, proficiencies, injuries and conditions of every character are saved. Closing waits for
// the saves that are still queued.
public interface CharacterStorage extends AutoCloseable {
    StatRepository repository();

    SkillRepository skillRepository();

    ProficiencyRepository proficiencyRepository();

    AfflictionRepository afflictionRepository();

    @Override
    void close();
}
