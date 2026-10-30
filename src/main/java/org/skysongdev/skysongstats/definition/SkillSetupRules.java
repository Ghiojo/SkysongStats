package org.skysongdev.skysongstats.definition;

// The /setup skill screen rules: how many skills a player picks, and the level a
// picked skill starts at.
public record SkillSetupRules(int picks, SkillLevel pickedLevel) {
}
