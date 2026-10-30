package org.skysongdev.skysongstats.proficiency;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tier node names: skysongstats.proficiency.(proficiency).(tier).
 */
class ProficiencyPermissionsTest {
    private final ProficienciesConfig config = ProficiencyTestConfig.load();
    private final ProficiencyDefinition axes = config.proficiencies().get("axes").orElseThrow();

    @Test
    void everyTierButTheFirstHasANode() {
        assertEquals(List.of("skysongstats.proficiency.axes.proficient", "skysongstats.proficiency.axes.adept",
                "skysongstats.proficiency.axes.master"), List.copyOf(ProficiencyPermissions.all(axes, config.tiers())));
    }

    @Test
    void aTierGivesItsNodeAndTheNodesBelow() {
        assertEquals(Set.of("skysongstats.proficiency.axes.proficient", "skysongstats.proficiency.axes.adept"),
                ProficiencyPermissions.reached(axes, config.tiers(), config.tiers().get("adept").orElseThrow()));
        assertTrue(ProficiencyPermissions.reached(axes, config.tiers(), config.tiers().first()).isEmpty());
    }
}
