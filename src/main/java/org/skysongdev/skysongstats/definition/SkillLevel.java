package org.skysongdev.skysongstats.definition;

// One level from skills.yml: a skill is at the highest level whose xp it has.
public record SkillLevel(String id, String name, String color, int xp) {
}
