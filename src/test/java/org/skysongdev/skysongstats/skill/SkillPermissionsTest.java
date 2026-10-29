package org.skysongdev.skysongstats.skill;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillsConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The skill level node names and which ones a character's XP gives.
 */
class SkillPermissionsTest {
    private final SkillsConfig config = SkillTestConfig.load();
    private final SkillDefinition tande = config.skills().get("tande").orElseThrow();

    @Test
    void nodesAreNamedAfterTheSkillAndLevelIds() {
        assertEquals("skysongstats.skill.tande.expert",
                SkillPermissions.node(tande, config.levels().get("expert").orElseThrow()));
    }

    @Test
    void everyLevelButTheFirstHasANode() {
        assertEquals(List.of("skysongstats.skill.tande.novice", "skysongstats.skill.tande.competent",
                "skysongstats.skill.tande.expert"), List.copyOf(SkillPermissions.all(tande, config.levels())));
    }

    @Test
    void aCharacterHasItsLevelAndEveryLevelBelow() {
        assertEquals(List.of(), List.copyOf(SkillPermissions.reached(tande, config.levels(), 15)));
        assertEquals(List.of("skysongstats.skill.tande.novice", "skysongstats.skill.tande.competent"),
                List.copyOf(SkillPermissions.reached(tande, config.levels(), 79)));
        assertEquals(SkillPermissions.all(tande, config.levels()), SkillPermissions.reached(tande, config.levels(), 128));
    }
}
