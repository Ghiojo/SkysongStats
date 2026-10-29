package org.skysongdev.skysongstats.definition;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit tests (no server needed) for {@link StatsConfigLoader}, the
 * stats.yml validation in {@link StatsValidator} and the lookups in
 * {@link StatDefinitions}.
 */
class StatsConfigLoaderTest {

    private static final Set<String> ITEMS = Set.of("IRON_SWORD", "FEATHER", "BARRIER");

    private static StatsConfig load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return StatsConfigLoader.load(config, ITEMS::contains);
    }

    private static List<String> errors(String yaml) {
        return assertThrows(DefinitionException.class, () -> load(yaml)).getErrors();
    }

    private static void assertError(String yaml, String expectedPart) {
        List<String> errors = errors(yaml);
        assertTrue(errors.stream().anyMatch(error -> error.contains(expectedPart)),
                "expected an error containing \"" + expectedPart + "\" but got " + errors);
    }

    @Test
    void plainStatsGetDefaults() {
        StatDefinition stat = load("stats:\n  luck: {}\n").stats().get("luck").orElseThrow();
        assertEquals(StatType.VALUE, stat.type());
        assertEquals("luck", stat.display().name());
        assertEquals("", stat.display().color());
        assertEquals(0, stat.baseline());
        assertTrue(stat.minimum().isEmpty());
        assertTrue(stat.maxId().isEmpty());
        assertTrue(stat.setupOptions().isEmpty());
        assertTrue(stat.commands().isEmpty());
        assertTrue(stat.itemTags().isEmpty());
        assertEquals(PermissionDefault.OP, stat.permissionDefault());
    }

    @Test
    void readsWhoHasTheStatsPermissionsByDefault() {
        StatDefinitions stats = load("""
                stats:
                  potion: {permission-default: true}
                  luck: {permission-default: false}
                  ac: {permission-default: "not op"}
                """).stats();
        assertEquals(PermissionDefault.TRUE, stats.get("potion").orElseThrow().permissionDefault());
        assertEquals(PermissionDefault.FALSE, stats.get("luck").orElseThrow().permissionDefault());
        assertEquals(PermissionDefault.NOT_OP, stats.get("ac").orElseThrow().permissionDefault());
    }

    @Test
    void rejectsABadPermissionDefault() {
        assertError("stats:\n  potion: {permission-default: everyone}\n",
                "stats.potion.permission-default: 'everyone' is not a permission default");
    }

    @Test
    void readsABarWithEverySetting() {
        StatDefinition hp = load("""
                stats:
                  con: {}
                  shield: {min: 0}
                  hp:
                    name: Health
                    color: "<red>"
                    aliases: [Health, life]
                    type: bar
                    max: "60 + {con} * 10"
                    start: max
                    min: -10
                    shield: Shield
                    item-tags:
                      - tag: Ruby
                        requires-charges: true
                    commands:
                      add: [heal]
                      damage: [hurt]
                """).stats().get("hp").orElseThrow();
        assertEquals(StatType.BAR, hp.type());
        assertEquals("Health", hp.display().name());
        assertEquals("<red>", hp.display().color());
        assertEquals(List.of("health", "life"), hp.display().aliases());
        assertEquals("60 + {con} * 10", hp.bar().max().getSource());
        assertTrue(hp.bar().startAtMax());
        assertEquals(-10, hp.minimum().orElseThrow());
        assertEquals("shield", hp.bar().shield());
        assertEquals(List.of(new ItemTagSource("ruby", true)), hp.itemTags());
        assertEquals(List.of("heal"), hp.commandsFor(StatAction.ADD));
        assertEquals(List.of("hurt"), hp.commandsFor(StatAction.DAMAGE));
        assertEquals(List.of(), hp.commandsFor(StatAction.SET));
        assertEquals("max_hp", hp.maxId().orElseThrow());
    }

    @Test
    void barsDefaultToAFloorOfZeroAndStartEmpty() {
        StatDefinition bar = load("stats:\n  mana: {type: bar, max: 10}\n").stats().get("mana").orElseThrow();
        assertEquals(0, bar.minimum().orElseThrow());
        assertFalse(bar.bar().startAtMax());
        assertEquals(0, bar.bar().start());
        assertNull(bar.bar().shield());
        assertFalse(bar.bar().canExceedMax());
        assertEquals(10, bar.bar().max().evaluate(id -> 0));
    }

    @Test
    void barsCanBeAllowedAboveTheirMax() {
        StatDefinition bar = load("stats:\n  arcrot: {type: bar, max: 35, can-exceed-max: true}\n")
                .stats().get("arcrot").orElseThrow();
        assertTrue(bar.bar().canExceedMax());
    }

    @Test
    void barMaximumsCanReadOtherBarMaximums() {
        StatsConfig config = load("""
                stats:
                  hp: {type: bar, max: 60}
                  wounds: {type: bar, max: "{max_hp} / 10"}
                """);
        assertEquals(List.of("max_hp"), List.copyOf(config.stats().get("wounds").orElseThrow().bar().max().references()));
    }

    @Test
    void readsSetupRulesAndSetupStats() {
        StatsConfig config = load("""
                setup: {points: 7, negative-points: 3}
                stats:
                  str:
                    setup: {icon: iron_sword, slot: 9, min: -3, max: 4, lore: ["<gray>Strong"]}
                  spd:
                    baseline: 6
                    setup: {icon: "model:skysong:boots", slot: 17, min: 0, max: 2}
                """);
        assertEquals(new SetupRules(7, 3), config.setupRules());
        StatSetup str = config.stats().get("str").orElseThrow().setup();
        assertEquals(new Icon("IRON_SWORD", null), str.icon());
        assertEquals(9, str.slot());
        assertEquals(-3, str.min());
        assertEquals(4, str.max());
        assertEquals(List.of("<gray>Strong"), str.lore());
        assertEquals("skysong:boots", config.stats().get("spd").orElseThrow().setup().icon().model());
        assertEquals(6, config.stats().get("spd").orElseThrow().baseline());
    }

    @Test
    void missingSetupRulesMeanNoPoints() {
        assertEquals(new SetupRules(0, 0), load("stats: {}\n").setupRules());
    }

    @Test
    void lookupsIgnoreCaseAndCoverIdsAliasesMaximumsAndCommands() {
        StatDefinitions stats = load("""
                stats:
                  str: {aliases: [strength]}
                  hp: {type: bar, max: 60, commands: {add: [heal]}}
                """).stats();
        assertEquals("str", stats.find("STRENGTH").orElseThrow().id());
        assertEquals("str", stats.find(" Str ").orElseThrow().id());
        assertTrue(stats.get("strength").isEmpty(), "get only takes ids");
        assertTrue(stats.find("dex").isEmpty());
        assertTrue(stats.find(null).isEmpty());
        assertEquals("hp", stats.findBarByMaxId("MAX_HP").orElseThrow().id());
        assertTrue(stats.findBarByMaxId("max_str").isEmpty(), "str is not a bar");
        assertTrue(stats.findBarByMaxId("hp").isEmpty());
        assertEquals(StatAction.ADD, stats.findCommand("Heal").orElseThrow().action());
        assertTrue(stats.findCommand("damage").isEmpty());
    }

    @Test
    void definitionsCannotBeModified() {
        StatsConfig config = load("stats:\n  str: {aliases: [strength]}\n");
        assertThrows(UnsupportedOperationException.class, () -> config.stats().all().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> config.stats().get("str").orElseThrow().display().aliases().clear());
    }

    @Test
    void reportsEveryMistakeAtOnce() {
        List<String> errors = errors("""
                stats:
                  str: {baseline: lots}
                  hp: {type: bar}
                  ac: {max: 10}
                """);
        assertEquals(3, errors.size(), errors.toString());
    }

    @Test
    void theExceptionMessageListsTheErrors() {
        DefinitionException error = assertThrows(DefinitionException.class, () -> load("stats:\n  Str: {}\n"));
        assertTrue(error.getMessage().startsWith("stats.yml has 1 error(s):"), error.getMessage());
        assertTrue(error.getMessage().contains("stats.Str"), error.getMessage());
    }

    @Test
    void rejectsAFileWithoutStats() {
        assertError("setup: {points: 7}\n", "must have a 'stats:' section");
    }

    @Test
    void rejectsBadIds() {
        assertError("stats:\n  Str: {}\n", "stats.Str: stat ids may only use");
        assertError("stats:\n  max_luck: {}\n", "reserved for bar maximums");
        assertError("stats:\n  str: 5\n", "stats.str: must be a section");
    }

    @Test
    void rejectsBadTypesAndNumbers() {
        assertError("stats:\n  str: {type: number}\n", "'number' is not a stat type");
        assertError("stats:\n  str: {baseline: 1.5}\n", "stats.str.baseline: '1.5' is not a whole number");
        assertError("stats:\n  hp: {type: bar, max: 10, start: half}\n", "stats.hp.start: 'half' is not a whole number");
        assertError("setup: {points: -1}\nstats: {}\n", "setup.points: can't be negative");
        assertError("stats:\n  hp: {type: bar, max: 10, can-exceed-max: sometimes}\n",
                "stats.hp.can-exceed-max: 'sometimes' must be true or false");
    }

    @Test
    void rejectsBarSettingsOnPlainValues() {
        assertError("stats:\n  ac: {max: 10}\n", "stats.ac.max: only bars have 'max'");
        assertError("stats:\n  ac: {start: 1}\n", "stats.ac.start: only bars");
        assertError("stats:\n  ac: {shield: str}\n", "stats.ac.shield: only bars");
        assertError("stats:\n  ac: {can-exceed-max: true}\n", "stats.ac.can-exceed-max: only bars");
    }

    @Test
    void rejectsBadFormulas() {
        assertError("stats:\n  hp: {type: bar}\n", "stats.hp.max: bars need a max");
        assertError("stats:\n  hp: {type: bar, max: \"60 + {con\"}\n", "stats.hp.max: Missing '}'");
        assertError("stats:\n  hp: {type: bar, max: \"60 + {con} * 10\"}\n", "stats.hp.max: {con} is not a stat id");
        assertError("stats:\n  str: {}\n  hp: {type: bar, max: \"{max_str}\"}\n", "only bars have a max_ id");
    }

    @Test
    void rejectsCircularMaximums() {
        assertError("""
                stats:
                  a: {type: bar, max: "{max_b}"}
                  b: {type: bar, max: "{max_c} + 1"}
                  c: {type: bar, max: "{max_a}"}
                """, "circular formula, max_a -> max_b -> max_c -> max_a");
        assertError("stats:\n  a: {type: bar, max: \"{max_a}\"}\n", "max_a -> max_a");
    }

    @Test
    void aMaximumReadingItsOwnCurrentValueIsNotCircular() {
        assertDoesNotThrow(() -> load("stats:\n  a: {type: bar, max: \"{a} + 1\"}\n"));
    }

    @Test
    void rejectsDuplicateOrInvalidAliases() {
        assertError("stats:\n  str: {}\n  dex: {aliases: [str]}\n", "stats.dex.aliases: 'str' is already used by stat 'str'");
        assertError("stats:\n  str: {aliases: [s]}\n  dex: {aliases: [s]}\n", "'s' is already used by stat 'str'");
        assertError("stats:\n  str: {aliases: [max_str]}\n", "not start with 'max_'");
        assertError("stats:\n  str: {aliases: [\"big str\"]}\n", "must use lowercase letters");
    }

    @Test
    void rejectsBadShields() {
        assertError("stats:\n  hp: {type: bar, max: 10, shield: hp}\n", "can't be its own shield");
        assertError("stats:\n  hp: {type: bar, max: 10, shield: armor}\n", "stats.hp.shield: 'armor' is not a stat id");
    }

    @Test
    void damageNeedsABarWithAShield() {
        assertError("stats:\n  hp: {type: bar, max: 10, commands: {damage: [hurt]}}\n", "damage needs a 'shield'");
        assertError("stats:\n  ac: {commands: {damage: [hurt]}}\n", "only bars with a 'shield' have damage");
    }

    @Test
    void rejectsBadCommands() {
        assertError("stats:\n  ac: {commands: {double: [x]}}\n", "stats.ac.commands.double: not an action");
        assertError("stats:\n  ac: {commands: {set: [\"set ac\"]}}\n", "'set ac' is not a valid command name");
        assertError("stats:\n  ac: {commands: {set: [setup]}}\n", "'/setup' is one of the plugin's own commands");
        assertError("""
                stats:
                  ac: {commands: {set: [ac]}}
                  hp: {type: bar, max: 10, commands: {add: [ac]}}
                """, "stats.hp.commands: '/ac' is already used by stat 'ac'");
    }

    @Test
    void rejectsBadItemTags() {
        assertError("stats:\n  arcrot: {item-tags: [{requires-charges: true}]}\n", "stats.arcrot.item-tags: every entry needs a 'tag'");
        assertError("stats:\n  arcrot: {item-tags: [{tag: \"bad tag\"}]}\n", "every entry needs a 'tag'");
    }

    @Test
    void rejectsBadSetupSettings() {
        assertError("stats:\n  hp: {type: bar, max: 10, setup: {icon: FEATHER, slot: 1, min: 0, max: 1}}\n",
                "bars can't be allocated in /setup");
        assertError("stats:\n  str: {setup: {slot: 1, min: 0, max: 1}}\n", "stats.str.setup.icon: missing");
        assertError("stats:\n  str: {setup: {icon: DIAMOND_ORE, slot: 1, min: 0, max: 1}}\n", "'DIAMOND_ORE' is not an item Material");
        assertError("stats:\n  str: {setup: {icon: \"model:bad\", slot: 1, min: 0, max: 1}}\n", "not a valid item model");
        assertError("stats:\n  str: {setup: {icon: FEATHER, min: 0, max: 1}}\n", "stats.str.setup.slot: missing");
        assertError("stats:\n  str: {setup: {icon: FEATHER, slot: 27, min: 0, max: 1}}\n", "must be between 0 and 26");
        assertError("stats:\n  str: {setup: {icon: FEATHER, slot: 4, min: 0, max: 1}}\n", "hold the /setup header");
        assertError("stats:\n  str: {setup: {icon: FEATHER, slot: 22, min: 0, max: 1}}\n", "hold the /setup header");
        assertError("stats:\n  str: {setup: {icon: FEATHER, slot: 1, min: 1, max: 2}}\n", "relative to the baseline");
        assertError("""
                stats:
                  str: {setup: {icon: FEATHER, slot: 9, min: 0, max: 1}}
                  dex: {setup: {icon: FEATHER, slot: 9, min: 0, max: 1}}
                """, "stats.dex.setup.slot: slot 9 is already used by stat 'str'");
    }
}
