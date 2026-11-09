package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.commands.stat.StatCommandFixture.Sent;
import org.skysongdev.skysongstats.definition.StatAction;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /height (the default stats.yml's height stat): metres or feet in, refused
 * outside 0.80 m to 3.60 m, and shown in metres and feet.
 */
class HeightCommandTest {
    private static final String SET = "skysongstats.stat.height.set";

    private StatCommandFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new StatCommandFixture();
        fixture.allow(fixture.alice, SET);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private void run(CommandSender sender, StatAction action, String amount, String target) {
        fixture.runner.run(sender, fixture.stats.definitions().get("height").orElseThrow(), action, amount, target);
    }

    private CommandTargets.Target alice() {
        return new CommandTargets.Target(fixture.alice, StatCommandFixture.character(fixture.alice));
    }

    private int height() {
        return fixture.value(fixture.alice, "height");
    }

    @Test
    void newCharactersAre180cm() {
        assertEquals(180, height());
    }

    @Test
    void setsTheHeightInMetresAndSaysItInMetresAndFeet() {
        run(fixture.alice, StatAction.SET, "1.75", null);
        assertEquals(175, height());
        Sent sent = fixture.messages.sent.get(0);
        assertEquals("stats.default.set.player", sent.key());
        assertEquals("1.75", sent.placeholders().get("value"));
        assertEquals("5'9\"", sent.placeholders().get("value_feet"));
        assertEquals("1.80", sent.placeholders().get("old"));
        assertEquals("1.75", sent.placeholders().get("amount"));
    }

    @Test
    void setsTheHeightInFeetAndInches() {
        run(fixture.alice, StatAction.SET, "6’2”", null);
        assertEquals(188, height());
    }

    @Test
    void usesTheHeightsOwnMessagesWhenMessagesYmlHasThem() {
        fixture.messages.known.add("stats.height.set.player");
        run(fixture.alice, StatAction.SET, "2", null);
        assertEquals(List.of("stats.height.set.player"), fixture.messages.keys());
    }

    @Test
    void refusesHeightsAboveTheMaxAndSaysTheMax() {
        run(fixture.alice, StatAction.SET, "3.61", null);
        assertEquals(180, height());
        assertEquals(List.of(new Sent("Alice", "stats.errors.too-tall", Map.of("max", "3.60", "max_feet", "11'10\""))),
                fixture.messages.sent);
    }

    @Test
    void refusesHeightsBelowTheMinAndSaysTheMin() {
        run(fixture.alice, StatAction.SET, "2'", null);
        assertEquals(180, height());
        assertEquals(List.of(new Sent("Alice", "stats.errors.too-short", Map.of("min", "0.80", "min_feet", "2'7\""))),
                fixture.messages.sent);
    }

    @Test
    void theLimitsThemselvesAreAllowed() {
        run(fixture.alice, StatAction.SET, "3.6", null);
        assertEquals(360, height());
        run(fixture.alice, StatAction.SET, "0.8", null);
        assertEquals(80, height());
    }

    @Test
    void addAndReduceAreRefusedWhenTheyWouldLeaveTheLimits() {
        fixture.allow(fixture.alice, "skysongstats.stat.height.add", "skysongstats.stat.height.reduce");
        run(fixture.alice, StatAction.ADD, "0.1", null);
        assertEquals(190, height());
        run(fixture.alice, StatAction.ADD, "2", null);
        assertEquals(190, height());
        run(fixture.alice, StatAction.REDUCE, "1.2", null);
        assertEquals(190, height());
        assertEquals(List.of("stats.default.add.player", "stats.errors.too-tall", "stats.errors.too-short"),
                fixture.messages.keys());
    }

    @Test
    void refusesTextThatIsNotAHeight() {
        run(fixture.alice, StatAction.SET, "180", null);
        assertEquals(180, height());
        assertEquals(List.of(new Sent("Alice", "stats.errors.too-tall", Map.of("max", "3.60", "max_feet", "11'10\""))),
                fixture.messages.sent);
        fixture.messages.sent.clear();
        run(fixture.alice, StatAction.SET, "tall", null);
        assertEquals(List.of(new Sent("Alice", "stats.errors.invalid-height", Map.of("input", "tall"))),
                fixture.messages.sent);
    }

    @Test
    void explainsAHeightSplitByASpace() {
        run(fixture.alice, StatAction.SET, "5'", "10\"");
        assertEquals(180, height());
        assertEquals(List.of(new Sent("Alice", "stats.errors.height-space", Map.of("input", "5' 10\""))),
                fixture.messages.sent);
    }

    @Test
    void settingSomeoneElsesHeightNeedsTheOtherPermission() {
        fixture.allow(fixture.bob, SET);
        run(fixture.bob, StatAction.SET, "2", "Alice");
        assertEquals(List.of("general.no-permission"), fixture.messages.keys());
        fixture.allow(fixture.bob, SET + ".other");
        run(fixture.bob, StatAction.SET, "2", "Alice");
        assertEquals(200, height());
        assertTrue(fixture.messages.keys().contains("stats.default.set.sender"));
    }

    @Test
    void theStatSheetLeavesOutStatsItIsToldToHide() {
        fixture.messages.texts.put(StatSheet.HIDDEN, List.of("height"));
        fixture.sheet.show(fixture.alice, alice());
        assertTrue(fixture.messages.sent.stream().noneMatch(sent -> "height".equals(sent.placeholders().get("stat"))));

        fixture.messages.sent.clear();
        fixture.messages.texts.put(StatSheet.HIDDEN, List.of());
        fixture.sheet.show(fixture.alice, alice());
        Sent auto = fixture.messages.sent.stream().filter(sent -> "height".equals(sent.placeholders().get("stat")))
                .findFirst().orElseThrow();
        assertEquals("1.80", auto.placeholders().get("value"));
    }
}
