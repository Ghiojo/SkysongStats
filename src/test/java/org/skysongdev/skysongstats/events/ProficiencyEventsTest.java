package org.skysongdev.skysongstats.events;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.character.ProficiencyFocus;
import org.skysongdev.skysongstats.character.ProficiencyFocusChange;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The proficiency events carry the change and have the handler list Bukkit needs.
 */
class ProficiencyEventsTest {

    private static final ProfileKey ALICE = new ProfileKey(UUID.randomUUID(), "main");
    private static final ProficiencyDefinition AXES = new ProficiencyDefinition("axes", "Axes", "", "martial", List.of(), 4);

    @Test
    void theTierChangeEventCarriesTheChange() {
        ProficiencyTierChange change = new ProficiencyTierChange(ALICE, AXES, new ProficiencyTier("untrained", "", "", 0),
                new ProficiencyTier("proficient", "", "", 0), true);
        ProficiencyTierChangeEvent event = new ProficiencyTierChangeEvent(change);
        assertEquals(change, event.getChange());
        assertSame(ProficiencyTierChangeEvent.getHandlerList(), event.getHandlers());
        assertNotNull(event.getHandlers());
    }

    @Test
    void theFocusChangeEventCarriesTheChange() {
        ProficiencyFocusChange change = new ProficiencyFocusChange(ALICE, null, new ProficiencyFocus(AXES, 0));
        ProficiencyFocusChangeEvent event = new ProficiencyFocusChangeEvent(change);
        assertEquals(change, event.getChange());
        assertTrue(change.focusBefore().isEmpty());
        assertEquals(AXES, change.focusAfter().orElseThrow().proficiency());
        assertSame(ProficiencyFocusChangeEvent.getHandlerList(), event.getHandlers());
    }
}
