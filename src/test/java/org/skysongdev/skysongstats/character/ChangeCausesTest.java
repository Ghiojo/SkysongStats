package org.skysongdev.skysongstats.character;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.api.event.ChangeCause;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChangeCausesTest {
    private final ChangeCauses causes = new ChangeCauses();

    @Test
    void aChangeOutsideAnyCauseIsACommand() {
        assertEquals(ChangeCause.COMMAND, causes.current());
    }

    @Test
    void duringSetsTheCauseAndPutsTheOldOneBack() {
        ChangeCause inside = causes.during(ChangeCause.API, causes::current);

        assertEquals(ChangeCause.API, inside);
        assertEquals(ChangeCause.COMMAND, causes.current());
    }

    @Test
    void theInnermostCauseWins() {
        ChangeCause[] seen = new ChangeCause[2];
        causes.during(ChangeCause.RELOAD, () -> {
            causes.during(ChangeCause.API, () -> {
                seen[0] = causes.current();
            });
            seen[1] = causes.current();
        });

        assertEquals(ChangeCause.API, seen[0]);
        assertEquals(ChangeCause.RELOAD, seen[1]);
    }

    @Test
    void theCauseIsPutBackWhenTheChangeFails() {
        assertThrows(IllegalStateException.class, () -> causes.during(ChangeCause.SETUP, () -> {
            throw new IllegalStateException("boom");
        }));

        assertEquals(ChangeCause.COMMAND, causes.current());
    }
}
