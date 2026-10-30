package org.skysongdev.skysongstats.proficiency;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.permissions.GeneratedPermissions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The proficiency tier nodes generated from proficiencies.yml, declared to the server.
 */
class ProficiencyPermissionRegistryTest {
    private ServerMock server;
    private ProficiencyPermissionRegistry registry;
    private Permission wildcard;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        wildcard = new Permission(GeneratedPermissions.WILDCARD);
        server.getPluginManager().addPermission(wildcard);
        registry = new ProficiencyPermissionRegistry(server.getPluginManager());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void declaresEveryTierButTheFirstForEveryProficiencyAsOpOnly() {
        registry.register(ProficiencyTestConfig.load());
        assertEquals(List.of(
                "skysongstats.proficiency.axes.proficient", "skysongstats.proficiency.axes.adept",
                "skysongstats.proficiency.axes.master",
                "skysongstats.proficiency.bows.proficient", "skysongstats.proficiency.bows.adept",
                "skysongstats.proficiency.bows.master"), registry.names());
        Permission master = server.getPluginManager().getPermission("skysongstats.proficiency.bows.master");
        assertNotNull(master);
        assertEquals(PermissionDefault.OP, master.getDefault());
        assertEquals("Given to characters at Master Bows or higher", master.getDescription());
    }

    @Test
    void theNodesAreUnderTheWildcard() {
        registry.register(ProficiencyTestConfig.load());
        assertEquals(Boolean.TRUE, wildcard.getChildren().get("skysongstats.proficiency.axes.proficient"));
    }

    @Test
    void unregisteringRemovesThem() {
        registry.register(ProficiencyTestConfig.load());
        registry.unregisterAll();
        assertTrue(registry.names().isEmpty());
        assertNull(server.getPluginManager().getPermission("skysongstats.proficiency.axes.proficient"));
    }
}
