package org.skysongdev.skysongstats.commands.affliction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** /condition on the shared affliction fixtures. */
class ConditionCommandTest extends AfflictionCommandFixture {
    private static final String PREFIX = "[SkysongStats] ";

    @Override
    protected String commandName() {
        return "condition";
    }

    private int level(String id) {
        return afflictions.condition(character(alice), id).orElseThrow().level();
    }

    @Test
    void unknownSubcommand() {
        assertEquals(PREFIX + "Incorrect Arguments! Usage: /condition (list/search/award/remove)", console("nope"));
    }

    @Test
    void anAwardIsLevelOneUnlessALevelIsGiven() {
        assertEquals(PREFIX + "Alice now has [Bleeding].", console("award", "Alice", "bleeding"));
        assertEquals(1, level("bleeding"));
        assertEquals(PREFIX + "You now have [Bleeding].", next(alice));

        assertEquals(PREFIX + "Alice's condition is now [Bleeding 3].", console("award", "Alice", "bleeding", "3"));
        assertEquals(3, level("bleeding"));
    }

    @Test
    void aBadLevelIsRefused() {
        assertEquals(PREFIX + "\"0\" must be a whole number of at least 1!", console("award", "Alice", "bleeding", "0"));
        assertEquals(PREFIX + "\"x\" must be a whole number of at least 1!", console("award", "Alice", "bleeding", "x"));
        assertTrue(afflictions.conditions(character(alice)).isEmpty());
    }

    @Test
    void anUnknownConditionIsRefused() {
        assertEquals(PREFIX + "There is no condition called nope!", console("award", "Alice", "nope"));
    }

    @Test
    void removeTakesBackADirectAward() {
        console("award", "Alice", "dazed");
        alice.nextComponentMessage();
        assertEquals(PREFIX + "Alice no longer has [Dazed].", console("remove", "Alice", "dazed"));
        assertEquals(PREFIX + "You no longer have [Dazed].", next(alice));
        assertTrue(afflictions.conditions(character(alice)).isEmpty());
    }

    @Test
    void aConditionOnlyAnInjuryGivesCantBeRemoved() {
        afflictions.awardInjury(character(alice), "cut");
        assertEquals(PREFIX + "Alice wasn't given [Bleeding] directly; it comes and goes with what gives it.",
                console("remove", "Alice", "bleeding"));
        assertEquals(2, level("bleeding"));
    }

    @Test
    void removingADirectAwardThatIsAlsoGivenSaysSo() {
        afflictions.awardInjury(character(alice), "cut");
        console("award", "Alice", "bleeding", "5");
        assertEquals(PREFIX + "Removed, but Alice still has [Bleeding] from an injury or another condition.",
                console("remove", "Alice", "bleeding"));
        assertEquals(2, level("bleeding"));
    }

    @Test
    void playersCantAwardWithoutPermission() {
        run(alice, "award", "Alice", "bleeding");
        assertEquals(PREFIX + "You don't have permission to use this command!", next(alice));
    }

    @Test
    void tabCompletesSubcommandsByPermissionAndConditionIds() {
        assertEquals(List.of("list", "search"), complete(alice, ""));
        assertEquals(List.of("list", "search", "award", "remove"), complete(console, ""));
        assertEquals(List.of("dazed"), complete(console, "award", "Alice", "d"));
    }
}
