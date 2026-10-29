package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.commands.stat.StatCommandFixture.Sent;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The checks and messages every generated stat command shares.
 */
class StatActionRunnerTest {
    private StatCommandFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new StatCommandFixture();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private StatDefinition stat(String id) {
        return fixture.stats.definitions().get(id).orElseThrow();
    }

    private void run(CommandSender sender, String stat, StatAction action, String amount, String target) {
        fixture.runner.run(sender, stat(stat), action, amount, target);
    }

    @Test
    void healingYourselfAddsHpAndTellsYou() {
        fixture.stats.apply(StatCommandFixture.character(fixture.alice), stat("hp"), StatAction.REDUCE, 20);
        fixture.allow(fixture.alice, "skysongstats.stat.hp.add");
        run(fixture.alice, "hp", StatAction.ADD, "5", null);
        assertEquals(45, fixture.value(fixture.alice, "hp"));
        Sent sent = fixture.messages.sent.get(0);
        assertEquals(List.of("stats.default.add.player"), fixture.messages.keys());
        assertEquals("Alice", sent.to());
        assertEquals(Map.of("amount", "5", "value", "45", "old", "40", "player", "Alice", "sender", "Alice",
                "name", "HP", "stat", "hp"), sent.placeholders());
    }

    @Test
    void aStatsOwnMessagesWinOverTheDefaults() {
        fixture.messages.known.add("stats.hp.add.player");
        fixture.allow(fixture.alice, "skysongstats.stat.hp.add");
        run(fixture.alice, "hp", StatAction.ADD, "5", null);
        assertEquals(List.of("stats.hp.add.player"), fixture.messages.keys());
    }

    @Test
    void usingItOnSomeoneElseNeedsTheOtherPermissionAndTellsBoth() {
        fixture.allow(fixture.bob, "skysongstats.stat.temphp.set");
        run(fixture.bob, "temphp", StatAction.SET, "5", "Alice");
        assertEquals(List.of("general.no-permission"), fixture.messages.keys());
        assertEquals("skysongstats.stat.temphp.set.other", fixture.messages.sent.get(0).placeholders().get("permission"));

        fixture.messages.sent.clear();
        fixture.allow(fixture.bob, "skysongstats.stat.temphp.set.other");
        run(fixture.bob, "temphp", StatAction.SET, "5", "Alice");
        assertEquals(5, fixture.value(fixture.alice, "temphp"));
        assertEquals(List.of(new Sent("Alice", "stats.default.set.player", fixture.messages.sent.get(0).placeholders()),
                new Sent("Bob", "stats.default.set.sender", fixture.messages.sent.get(0).placeholders())), fixture.messages.sent);
    }

    @Test
    void namingYourselfOnlyNeedsTheSelfPermission() {
        fixture.allow(fixture.alice, "skysongstats.stat.ac.set");
        run(fixture.alice, "ac", StatAction.SET, "14", "Alice");
        assertEquals(14, fixture.value(fixture.alice, "ac"));
        assertEquals(List.of("stats.default.set.player"), fixture.messages.keys());
    }

    @Test
    void withoutThePermissionNothingChanges() {
        run(fixture.alice, "hp", StatAction.REDUCE, "5", null);
        assertEquals(60, fixture.value(fixture.alice, "hp"));
        assertEquals(List.of("general.no-permission"), fixture.messages.keys());
    }

    @Test
    void theConsoleMustNameAPlayer() {
        run(fixture.server.getConsoleSender(), "hp", StatAction.REDUCE, "5", null);
        assertEquals(List.of("general.console-needs-player"), fixture.messages.keys());

        fixture.messages.sent.clear();
        run(fixture.server.getConsoleSender(), "hp", StatAction.REDUCE, "5", "Alice");
        assertEquals(55, fixture.value(fixture.alice, "hp"));
        assertEquals(List.of("stats.default.reduce.player", "stats.default.reduce.sender"), fixture.messages.keys());
    }

    @Test
    void unknownPlayersAndPlayersWithoutACharacterAreReported() {
        run(fixture.server.getConsoleSender(), "hp", StatAction.ADD, "5", "Nobody");
        run(fixture.server.getConsoleSender(), "hp", StatAction.ADD, "5", "Bob");
        assertEquals(List.of("general.player-not-found", "stats.errors.no-character"), fixture.messages.keys());
    }

    @Test
    void amountsMustBeWholeNumbersAndOnlySetTakesNegatives() {
        fixture.allow(fixture.alice, "skysongstats.stat.hp.add", "skysongstats.stat.str.set");
        run(fixture.alice, "hp", StatAction.ADD, "five", null);
        run(fixture.alice, "hp", StatAction.ADD, "-5", null);
        assertEquals(List.of("general.not-a-number", "stats.errors.negative-amount"), fixture.messages.keys());

        run(fixture.alice, "str", StatAction.SET, "-2", null);
        assertEquals(-2, fixture.value(fixture.alice, "str"));
    }

    @Test
    void damageGoesThroughTempHpFirst() {
        fixture.stats.apply(StatCommandFixture.character(fixture.alice), stat("temphp"), StatAction.SET, 10);
        run(fixture.server.getConsoleSender(), "hp", StatAction.DAMAGE, "15", "Alice");
        assertEquals(0, fixture.value(fixture.alice, "temphp"));
        assertEquals(55, fixture.value(fixture.alice, "hp"));
        assertEquals("55", fixture.messages.sent.get(0).placeholders().get("value"));
    }

    @Test
    void damageNeedsAStatWithAShield() {
        run(fixture.server.getConsoleSender(), "potion", StatAction.DAMAGE, "1", "Alice");
        assertEquals(List.of("stats.errors.no-shield"), fixture.messages.keys());
        assertEquals(0, fixture.value(fixture.alice, "potion"));
    }
}
