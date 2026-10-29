package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /stats set|add|reduce|damage for any stat, by id or alias.
 */
class StatsRootCommandTest {
    private StatCommandFixture fixture;
    private StatsRootCommand stats;
    private Command command;

    @BeforeEach
    void setUp() {
        fixture = new StatCommandFixture();
        stats = new StatsRootCommand(fixture.stats, fixture.runner, fixture.messages, fixture::onlineNames);
        command = new Command("stats") {
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

    private void run(String... args) {
        stats.onCommand(fixture.server.getConsoleSender(), command, "stats", args);
    }

    // Bug 7: /stats set didn't set the value.
    @Test
    void setsAnyStatByIdOrAlias() {
        run("set", "strength", "3", "Alice");
        run("add", "SPD", "2", "Alice");
        run("reduce", "hp", "15", "Alice");
        assertEquals(3, fixture.value(fixture.alice, "str"));
        assertEquals(8, fixture.value(fixture.alice, "spd"));
        assertEquals(45, fixture.value(fixture.alice, "hp"));
    }

    @Test
    void damageWorksForBarsWithAShield() {
        run("damage", "hp", "5", "Alice");
        assertEquals(55, fixture.value(fixture.alice, "hp"));
    }

    @Test
    void wrongArgumentsShowTheUsage() {
        run();
        run("heal", "hp", "5");
        run("set", "str");
        run("set", "str", "1", "Alice", "extra");
        assertEquals(List.of("stats.errors.usage", "stats.errors.usage", "stats.errors.usage", "stats.errors.usage"),
                fixture.messages.keys());
    }

    @Test
    void unknownStatsAreReported() {
        run("set", "luck", "3", "Alice");
        assertEquals(List.of("stats.errors.unknown-stat"), fixture.messages.keys());
        assertEquals("luck", fixture.messages.sent.get(0).placeholders().get("stat"));
    }

    @Test
    void tabCompletesActionsStatsAndPlayers() {
        assertEquals(List.of("reduce"), stats.onTabComplete(fixture.alice, command, "stats", new String[]{"re"}));
        assertEquals(List.of("str", "strength", "spd", "speed"),
                stats.onTabComplete(fixture.alice, command, "stats", new String[]{"set", "s"}));
        assertEquals(List.of("Bob"), stats.onTabComplete(fixture.alice, command, "stats", new String[]{"set", "str", "1", "b"}));
        assertTrue(stats.onTabComplete(fixture.alice, command, "stats", new String[]{"set", "str", ""}).isEmpty());
    }
}
