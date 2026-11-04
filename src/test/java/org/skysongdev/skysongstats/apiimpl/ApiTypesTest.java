package org.skysongdev.skysongstats.apiimpl;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.DowntimeChange;
import org.skysongdev.skysongstats.api.DowntimeTarget;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.api.event.DowntimeChangeEvent;
import org.skysongdev.skysongstats.service.downtime.DowntimePool;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// The conversions the events rely on.
class ApiTypesTest {
    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void aGlobalDowntimeChangeNamesThePlayer() {
        DowntimeChange change = ApiTypes.downtimeChange(new org.skysongdev.skysongstats.service.downtime.DowntimeChange(
                PLAYER, DowntimePool.GLOBAL, null, 1, 4,
                org.skysongdev.skysongstats.service.downtime.DowntimeChange.Cause.ADD));

        assertEquals(new DowntimeChange(DowntimeTarget.global(PLAYER), DowntimeChange.Kind.ADD, 1, 4), change);
    }

    @Test
    void aCharacterDowntimeChangeNamesTheCharacter() {
        DowntimeChange change = ApiTypes.downtimeChange(new org.skysongdev.skysongstats.service.downtime.DowntimeChange(
                PLAYER, DowntimePool.CHARACTER, "Mage", 1, 3,
                org.skysongdev.skysongstats.service.downtime.DowntimeChange.Cause.RESET));

        assertEquals(DowntimeTarget.character(new CharacterId(PLAYER, "Mage")), change.target());
        assertEquals(DowntimeChange.Kind.RESET, change.kind());
    }

    @Test
    void theDowntimeEventCarriesTheChangeAndCause() {
        DowntimeChange change = new DowntimeChange(DowntimeTarget.global(PLAYER), DowntimeChange.Kind.REMOVE, 3, 2);
        DowntimeChangeEvent event = new DowntimeChangeEvent(change, ChangeCause.COMMAND);

        assertEquals(change, event.getChange());
        assertEquals(ChangeCause.COMMAND, event.getCause());
        assertSame(DowntimeChangeEvent.getHandlerList(), event.getHandlers());
    }
}
