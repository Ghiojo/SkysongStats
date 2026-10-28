package org.skysongdev.skysongstats.Utils;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.Utils.Utils.Skills;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests (no server needed) for the existing {@link Skills} enum.
 * They pin down current behaviour so later refactors can't change it silently.
 */
class SkillsEnumTest {

    @Test
    void fromIntReturnsTheSkillWithThatValue() {
        for (Skills skill : Skills.values()) {
            assertEquals(skill, Skills.fromInt(skill.getValue()));
        }
    }

    @Test
    void fromIntReturnsNullForUnknownIds() {
        assertNull(Skills.fromInt(-1));
        assertNull(Skills.fromInt(Skills.values().length));
    }

    @Test
    void everySkillHasAUniqueId() {
        Set<String> ids = new HashSet<>();
        for (Skills skill : Skills.values()) {
            assertTrue(ids.add(Skills.getSkillid(skill)), "duplicate id for " + skill);
        }
    }
}
