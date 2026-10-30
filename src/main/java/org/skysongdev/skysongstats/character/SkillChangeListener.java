package org.skysongdev.skysongstats.character;

// Told about every skill XP change, e.g. to update the LuckPerms level nodes.
@FunctionalInterface
public interface SkillChangeListener {
    void onChange(SkillChange change);
}
