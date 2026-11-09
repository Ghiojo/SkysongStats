package org.skysongdev.skysongstats.commands.affliction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** /injury on the shared affliction fixtures. */
class InjuryCommandTest extends AfflictionCommandFixture {
    private static final String PREFIX = "[SkysongStats] ";
    private static final String NO_PERMISSION = PREFIX + "You don't have permission to use this command!";

    @Override
    protected String commandName() {
        return "injury";
    }

    @Test
    void unknownSubcommand() {
        assertEquals(PREFIX + "Incorrect Arguments! Usage: /injury (list/award/remove/discover/forget)",
                console("nope"));
    }

    @Test
    void staffSubcommandsNeedAPlayerAndAnInjury() {
        assertEquals(PREFIX + "Usage: /injury (award/remove/discover/forget) (player) (injury)",
                console("award", "Alice"));
    }

    @Test
    void anAwardGivesAndTeachesTheInjuryAndSendsItsMessage() {
        assertEquals(PREFIX + "Alice now has Broken Leg.", console("award", "Alice", "broken-leg"));

        assertTrue(afflictions.hasInjury(character(alice), "broken-leg"));
        assertTrue(afflictions.knows(character(alice), "broken-leg"));
        // No character sheet, so the player's name stands in.
        assertEquals("Alice now has a Broken Leg", next(alice));
        assertNull(alice.nextComponentMessage());
    }

    @Test
    void anInjuryWithoutAMessageSendsNothingToThePlayer() {
        console("award", "Alice", "cut");
        assertNull(alice.nextComponentMessage());
    }

    @Test
    void awardingAgainSaysSo() {
        console("award", "Alice", "cut");
        assertEquals(PREFIX + "Alice already had Cut; it now counts from now.", console("award", "Alice", "CUT"));
    }

    @Test
    void anUnknownInjuryIsRefused() {
        assertEquals(PREFIX + "There is no injury called nope!", console("award", "Alice", "nope"));
    }

    @Test
    void anUnknownPlayerIsRefused() {
        assertEquals(PREFIX + "Player Carol not found!", console("award", "Carol", "cut"));
    }

    @Test
    void playersCantAwardWithoutPermission() {
        run(alice, "award", "Bob", "cut");
        assertEquals(NO_PERMISSION, next(alice));
        assertFalse(afflictions.hasInjury(character(bob), "cut"));
    }

    @Test
    void removeTakesItAwayAndTellsThePlayer() {
        console("award", "Alice", "cut");
        assertEquals(PREFIX + "Alice no longer has Cut.", console("remove", "Alice", "cut"));
        assertEquals(PREFIX + "You no longer have Cut.", next(alice));
        assertTrue(afflictions.knows(character(alice), "cut"));
        assertEquals(PREFIX + "Alice doesn't have Cut.", console("remove", "Alice", "cut"));
    }

    @Test
    void discoverAndForget() {
        assertEquals(PREFIX + "Alice now knows Bruise.", console("discover", "Alice", "bruise"));
        assertEquals(PREFIX + "You now know about Bruise.", next(alice));
        assertEquals(PREFIX + "Alice already knew Bruise.", console("discover", "Alice", "bruise"));
        assertFalse(afflictions.hasInjury(character(alice), "bruise"));

        assertEquals(PREFIX + "Alice no longer knows Bruise.", console("forget", "Alice", "bruise"));
        assertEquals(PREFIX + "Alice doesn't know Bruise.", console("forget", "Alice", "bruise"));
    }

    @Test
    void anInjuryCantBeForgottenWhileTheCharacterHasIt() {
        afflictions.awardInjury(character(alice), "cut");
        assertEquals(PREFIX + "Alice has Cut right now, remove it first.", console("forget", "Alice", "cut"));
        assertTrue(afflictions.knows(character(alice), "cut"));
    }

    @Test
    void playersListTheirOwnInjuries() {
        run(alice);
        assertEquals(PREFIX + "Alice has no injuries.", next(alice));
        afflictions.awardInjury(character(alice), "cut");
        afflictions.awardInjury(character(alice), "bruise");
        alice.nextComponentMessage();

        run(alice, "list");
        assertEquals(List.of(PREFIX + "Alice's injuries:", " - Cut", " - Bruise"), all(alice));
    }

    @Test
    void listingSomeoneElseNeedsPermission() {
        run(alice, "list", "Bob");
        assertEquals(NO_PERMISSION, next(alice));
    }

    @Test
    void tabCompletesSubcommandsByPermissionAndInjuryIds() {
        assertEquals(List.of("list"), complete(alice, ""));
        assertEquals(List.of("list", "award", "remove", "discover", "forget"), complete(console, ""));
        assertEquals(List.of("broken-leg", "bruise"), complete(console, "award", "Alice", "b"));
        assertEquals(List.of("Alice"), complete(console, "list", "Al"));
    }
}
