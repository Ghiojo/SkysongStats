package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The permission nodes generated from stats.yml, declared to the server.
 */
class StatPermissionRegistryTest {
    private ServerMock server;
    private StatPermissionRegistry registry;
    private Permission wildcard;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        wildcard = new Permission(StatPermissionRegistry.WILDCARD);
        server.getPluginManager().addPermission(wildcard);
        registry = new StatPermissionRegistry(server.getPluginManager());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static StatDefinitions stats(String yaml) {
        return StatsConfigLoader.load(YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml)), name -> true)
                .stats();
    }

    @Test
    void declaresEveryActionForYourselfAndOthersAsOpOnly() {
        registry.register(stats("stats:\n  str: {name: Strength}\n"));
        assertEquals(List.of("skysongstats.stat.str.set", "skysongstats.stat.str.set.other",
                "skysongstats.stat.str.add", "skysongstats.stat.str.add.other",
                "skysongstats.stat.str.reduce", "skysongstats.stat.str.reduce.other"), registry.names());
        Permission add = server.getPluginManager().getPermission("skysongstats.stat.str.add");
        assertNotNull(add);
        assertEquals(PermissionDefault.OP, add.getDefault());
    }

    @Test
    void aStatsPermissionDefaultAppliesToAllItsNodes() {
        registry.register(stats("stats:\n  potion: {permission-default: true}\n"));
        for (String name : registry.names()) {
            assertEquals(PermissionDefault.TRUE, server.getPluginManager().getPermission(name).getDefault(), name);
        }
    }

    @Test
    void damageOnlyExistsForBarsWithAShield() {
        registry.register(stats("""
                stats:
                  temphp: {min: 0}
                  hp: {type: bar, max: 10, shield: temphp}
                """));
        assertTrue(registry.names().contains("skysongstats.stat.hp.damage.other"));
        assertFalse(registry.names().contains("skysongstats.stat.temphp.damage"));
    }

    @Test
    void theNodesAreUnderTheWildcard() {
        registry.register(stats("stats:\n  str: {}\n"));
        assertEquals(Boolean.TRUE, wildcard.getChildren().get("skysongstats.stat.str.set.other"));
    }

    @Test
    void aReloadSwapsTheNodes() {
        registry.register(stats("stats:\n  str: {}\n"));
        registry.register(stats("stats:\n  luck: {}\n"));
        assertNull(server.getPluginManager().getPermission("skysongstats.stat.str.set"));
        assertNotNull(server.getPluginManager().getPermission("skysongstats.stat.luck.set"));
        assertFalse(wildcard.getChildren().containsKey("skysongstats.stat.str.set"));
        assertTrue(wildcard.getChildren().containsKey("skysongstats.stat.luck.set"));
    }

    @Test
    void unregisterAllRemovesThem() {
        registry.register(stats("stats:\n  str: {}\n"));
        registry.unregisterAll();
        assertNull(server.getPluginManager().getPermission("skysongstats.stat.str.set"));
        assertTrue(wildcard.getChildren().isEmpty());
    }
}
