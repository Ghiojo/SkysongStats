package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registering the command names from stats.yml, and running them like a player
 * would. The default file reproduces today's HP, temp HP, AC and potion commands.
 */
class StatCommandRegistryTest {
    private static final Set<String> TODAYS_COMMANDS = Set.of(
            "heal", "addhp", "reducehp", "rhp", "damage", "dhp", "temphp", "ac", "potionsickness");

    private StatCommandFixture fixture;
    private StatCommandRegistry registry;

    @BeforeEach
    void setUp() {
        fixture = new StatCommandFixture();
        registry = new StatCommandRegistry(fixture.server.getCommandMap(), "skysongstats",
                label -> new StatAliasCommand(label, fixture.stats, fixture.runner, fixture.messages, fixture::onlineNames));
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private Command known(String label) {
        return fixture.server.getCommandMap().getKnownCommands().get(label);
    }

    @Test
    void registersEveryCommandNameInTheDefaultFile() {
        assertEquals(List.of(), registry.register(fixture.stats.definitions()));
        assertEquals(TODAYS_COMMANDS, Set.copyOf(registry.labels()));
        for (String label : TODAYS_COMMANDS) {
            assertInstanceOf(StatAliasCommand.class, known(label), label);
            assertInstanceOf(StatAliasCommand.class, known("skysongstats:" + label), label);
        }
    }

    @Test
    void theCommandsRunTheirStatActions() {
        registry.register(fixture.stats.definitions());
        fixture.allow(fixture.alice, "skysongstats.stat.hp.reduce", "skysongstats.stat.hp.add",
                "skysongstats.stat.potion.set");
        fixture.server.dispatchCommand(fixture.alice, "rhp 30");
        fixture.server.dispatchCommand(fixture.alice, "heal 10");
        fixture.server.dispatchCommand(fixture.alice, "potionsickness 4");
        assertEquals(40, fixture.value(fixture.alice, "hp"));
        assertEquals(4, fixture.value(fixture.alice, "potion"));
    }

    @Test
    void wrongArgumentsShowTheUsage() {
        registry.register(fixture.stats.definitions());
        fixture.server.dispatchCommand(fixture.alice, "heal");
        fixture.server.dispatchCommand(fixture.alice, "heal 1 Alice extra");
        assertEquals(List.of("stats.errors.usage", "stats.errors.usage"), fixture.messages.keys());
        assertEquals("/heal <amount> [player]", fixture.messages.sent.get(0).placeholders().get("usage"));
    }

    @Test
    void tabCompletesPlayerNamesAfterTheAmount() {
        registry.register(fixture.stats.definitions());
        Command heal = known("heal");
        assertEquals(List.of("Alice"), heal.tabComplete(fixture.alice, "heal", new String[]{"5", "al"}));
        assertEquals(List.of(), heal.tabComplete(fixture.alice, "heal", new String[]{"5"}));
    }

    @Test
    void reloadingSwapsTheCommandNames() throws InvalidConfigurationException {
        registry.register(fixture.stats.definitions());
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("stats:\n  hp: {type: bar, max: 60, start: max, commands: {add: [mend]}}\n");
        StatDefinitions reloaded = StatsConfigLoader.load(config, name -> true).stats();
        fixture.stats.setDefinitions(reloaded);
        registry.register(reloaded);

        assertEquals(List.of("mend"), registry.labels());
        assertNull(known("heal"));
        assertNull(known("skysongstats:heal"));
        assertInstanceOf(StatAliasCommand.class, known("mend"));
    }

    @Test
    void namesAnotherPluginOwnsOnlyGetThePrefixedForm() {
        Command other = new Command("heal") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                return true;
            }
        };
        fixture.server.getCommandMap().register("heal", "otherplugin", other);
        assertEquals(List.of("heal"), registry.register(fixture.stats.definitions()));
        assertEquals(other, known("heal"));
        assertInstanceOf(StatAliasCommand.class, known("skysongstats:heal"));

        registry.unregisterAll();
        assertEquals(other, known("heal"), "the other plugin's command is left alone");
        assertNull(known("skysongstats:heal"));
        assertTrue(registry.labels().isEmpty());
        assertFalse(fixture.server.getCommandMap().getKnownCommands().containsKey("skysongstats:rhp"));
    }
}
