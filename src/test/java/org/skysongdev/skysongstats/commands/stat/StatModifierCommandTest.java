package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.storage.StoredModifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /modifier add|del|view on the stats from stats.yml.
 */
class StatModifierCommandTest {
    private StatCommandFixture fixture;
    private StatModifierCommand modifier;
    private Command command;

    @BeforeEach
    void setUp() {
        fixture = new StatCommandFixture();
        modifier = new StatModifierCommand(fixture.stats, fixture.targets, fixture.messages, fixture::onlineNames);
        command = new Command("modifier") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                return true;
            }
        };
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private void run(CommandSender sender, String... args) {
        modifier.onCommand(sender, command, "modifier", args);
    }

    private List<StoredModifier> modifiers() {
        return fixture.stats.sheet(StatCommandFixture.character(fixture.alice)).orElseThrow().modifiers();
    }

    private StatCommandFixture.Sent last() {
        return fixture.messages.sent.get(fixture.messages.sent.size() - 1);
    }

    @Test
    void addsAModifierToAStatByIdOrAlias() {
        run(fixture.alice, "add", "strength", "2");
        run(fixture.alice, "add", "STR", "-1");
        assertEquals(1, fixture.value(fixture.alice, "str"));
        assertEquals(List.of("str", "str"), modifiers().stream().map(StoredModifier::target).toList());
        assertEquals("modifier.added", last().key());
        assertEquals("-1", last().placeholders().get("amount"));
        assertEquals("str", last().placeholders().get("target"));
    }

    @Test
    void addsAModifierToABarMaxWithItsOwnMessage() {
        int before = fixture.value(fixture.alice, "max_hp");
        run(fixture.alice, "add", "max_hp", "5");
        assertEquals(before + 5, fixture.value(fixture.alice, "max_hp"));
        assertEquals("modifier.added-max", last().key());
        assertEquals("max_hp", last().placeholders().get("target"));
    }

    @Test
    void refusesWhatIsNotAStatOrBarMax() {
        run(fixture.alice, "add", "luck", "2");
        run(fixture.alice, "add", "max_str", "2");
        assertEquals(List.of("modifier.not-a-target", "modifier.not-a-target"), fixture.messages.keys());
        assertTrue(modifiers().isEmpty());
    }

    @Test
    void refusesAnAmountThatIsNotANumber() {
        run(fixture.alice, "add", "str", "two");
        assertEquals(List.of("general.not-a-number"), fixture.messages.keys());
        assertTrue(modifiers().isEmpty());
    }

    @Test
    void actingOnSomeoneElseNeedsTheOtherPermission() {
        run(fixture.bob, "add", "str", "2", "Alice");
        assertEquals("general.no-permission", last().key());
        assertTrue(modifiers().isEmpty());

        fixture.allow(fixture.bob, "skysongstats.modifier.add.other");
        run(fixture.bob, "add", "str", "2", "Alice");
        assertEquals(1, modifiers().size());
    }

    @Test
    void deletesByTheNumberViewShows() {
        run(fixture.alice, "add", "str", "1");
        run(fixture.alice, "add", "dex", "2");
        run(fixture.alice, "del", "2");
        assertEquals(List.of("str"), modifiers().stream().map(StoredModifier::target).toList());
        assertEquals("modifier.removed", last().key());
        assertEquals("2", last().placeholders().get("index"));

        run(fixture.alice, "delete", "1");
        assertTrue(modifiers().isEmpty());
    }

    @Test
    void deletingANumberWithNoModifierChangesNothing() {
        run(fixture.alice, "add", "str", "1");
        run(fixture.alice, "del", "0");
        run(fixture.alice, "del", "2");
        assertEquals(List.of("modifier.added", "modifier.no-such-modifier", "modifier.no-such-modifier"),
                fixture.messages.keys());
        assertEquals(1, modifiers().size());
    }

    @Test
    void deletingFromSomeoneElseNeedsTheOtherPermission() {
        run(fixture.alice, "add", "str", "1");
        run(fixture.bob, "del", "1", "Alice");
        assertEquals("general.no-permission", last().key());
        fixture.allow(fixture.bob, "skysongstats.modifier.del.other");
        run(fixture.bob, "del", "1", "Alice");
        assertTrue(modifiers().isEmpty());
    }

    @Test
    void viewShowsTenModifiersPerPageNumberedFromOne() {
        for (int i = 1; i <= 12; i++) {
            run(fixture.alice, "add", "max_hp", Integer.toString(i));
        }
        fixture.messages.sent.clear();
        run(fixture.alice, "view", "2");
        assertEquals(List.of("modifier.view.header", "modifier.view.line-max", "modifier.view.line-max",
                "modifier.view.footer"), fixture.messages.keys());
        assertEquals("11", fixture.messages.sent.get(1).placeholders().get("index"));
        assertEquals("12", fixture.messages.sent.get(2).placeholders().get("amount"));
        assertEquals("2", fixture.messages.sent.get(0).placeholders().get("page"));
    }

    @Test
    void viewSaysWhenThereAreNoModifiers() {
        run(fixture.alice, "view");
        assertEquals(List.of("modifier.view.header", "modifier.view.empty", "modifier.view.footer"),
                fixture.messages.keys());
        assertEquals("Alice", fixture.messages.sent.get(0).placeholders().get("player"));
    }

    @Test
    void viewRefusesABadPage() {
        run(fixture.alice, "view", "0");
        run(fixture.alice, "view", "first");
        assertEquals(List.of("modifier.invalid-page", "general.not-a-number"), fixture.messages.keys());
    }

    @Test
    void viewingSomeoneElseNeedsTheOtherPermission() {
        run(fixture.bob, "view", "1", "Alice");
        assertEquals("general.no-permission", last().key());
        fixture.allow(fixture.bob, "skysongstats.modifier.view.other");
        fixture.messages.sent.clear();
        run(fixture.bob, "view", "1", "Alice");
        assertEquals("modifier.view.header", fixture.messages.sent.get(0).key());
    }

    @Test
    void wrongArgumentsShowTheUsage() {
        run(fixture.alice);
        run(fixture.alice, "remove", "1");
        run(fixture.alice, "add", "str");
        run(fixture.alice, "del");
        assertEquals(List.of("stats.errors.usage", "stats.errors.usage", "stats.errors.usage",
                "stats.errors.usage"), fixture.messages.keys());
    }

    @Test
    void tabCompletesSubcommandsTargetsAndPlayers() {
        CommandSender sender = fixture.alice;
        assertEquals(List.of("add"), modifier.onTabComplete(sender, command, "modifier", new String[]{"a"}));
        List<String> targets = modifier.onTabComplete(sender, command, "modifier", new String[]{"add", "max_"});
        assertTrue(targets.contains("max_hp"), targets.toString());
        assertTrue(targets.stream().allMatch(name -> name.startsWith("max_")), targets.toString());
        assertEquals(List.of("Bob"), modifier.onTabComplete(sender, command, "modifier",
                new String[]{"view", "1", "B"}));
        assertEquals(List.of(), modifier.onTabComplete(sender, command, "modifier", new String[]{"add", "str", ""}));
    }
}
