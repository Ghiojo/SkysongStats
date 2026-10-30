package org.skysongdev.skysongstats.setup;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pick rules of the /setup skill screen, on the default skills.yml (2 picks).
 */
class SkillPicksTest {
    private final SkillsConfig config = SetupTestData.defaultSkills();
    private final SkillPicks picks = new SkillPicks(config.setup().picks());

    private SkillDefinition skill(String id) {
        return config.skills().get(id).orElseThrow();
    }

    @Test
    void startsWithEveryPickLeft() {
        assertEquals(2, picks.left());
        assertFalse(picks.canConfirm());
        assertTrue(picks.picked().isEmpty());
    }

    @Test
    void picksUpToTheAllowedNumber() {
        assertTrue(picks.pick(skill("farming")));
        assertTrue(picks.pick(skill("tande")));
        assertFalse(picks.pick(skill("mining")));
        assertEquals(List.of(skill("farming"), skill("tande")), picks.picked());
        assertTrue(picks.canConfirm());
    }

    @Test
    void aSkillCanOnlyBePickedOnce() {
        assertTrue(picks.pick(skill("farming")));
        assertFalse(picks.pick(skill("farming")));
        assertEquals(1, picks.left());
    }

    @Test
    void unpickingGivesThePickBack() {
        picks.pick(skill("farming"));
        assertTrue(picks.unpick(skill("farming")));
        assertFalse(picks.unpick(skill("farming")));
        assertFalse(picks.isPicked(skill("farming")));
        assertEquals(2, picks.left());
    }

    @Test
    void resetClearsThePicks() {
        picks.pick(skill("farming"));
        picks.reset();
        assertEquals(2, picks.left());
    }

    @Test
    void noPicksMeansItCanConfirmAtOnce() {
        assertTrue(new SkillPicks(0).canConfirm());
    }
}
