package org.skysongdev.skysongstats.skill;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.skysongdev.skysongstats.permissions.GeneratedPermissions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The skill level nodes generated from skills.yml, declared to the server.
 */
class SkillPermissionRegistryTest {
    private ServerMock server;
    private SkillPermissionRegistry registry;
    private Permission wildcard;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        wildcard = new Permission(GeneratedPermissions.WILDCARD);
        server.getPluginManager().addPermission(wildcard);
        registry = new SkillPermissionRegistry(server.getPluginManager());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void declaresEveryLevelButTheFirstForEverySkillAsOpOnly() {
        registry.register(SkillTestConfig.load());
        assertEquals(List.of(
                "skysongstats.skill.farming.novice", "skysongstats.skill.farming.competent",
                "skysongstats.skill.farming.expert",
                "skysongstats.skill.tande.novice", "skysongstats.skill.tande.competent",
                "skysongstats.skill.tande.expert"), registry.names());
        Permission expert = server.getPluginManager().getPermission("skysongstats.skill.tande.expert");
        assertNotNull(expert);
        assertEquals(PermissionDefault.OP, expert.getDefault());
        assertEquals("Given to characters at Expert Transportation and Exploration or higher", expert.getDescription());
    }

    @Test
    void theNodesAreUnderTheWildcard() {
        registry.register(SkillTestConfig.load());
        assertEquals(Boolean.TRUE, wildcard.getChildren().get("skysongstats.skill.farming.novice"));
    }

    @Test
    void aReloadSwapsTheNodes() {
        registry.register(SkillTestConfig.load());
        registry.register(SkillTestConfig.load(SkillTestConfig.SKILLS.replace("farming:", "mining:")));
        assertNull(server.getPluginManager().getPermission("skysongstats.skill.farming.novice"));
        assertNotNull(server.getPluginManager().getPermission("skysongstats.skill.mining.novice"));
        assertFalse(wildcard.getChildren().containsKey("skysongstats.skill.farming.novice"));
        assertTrue(wildcard.getChildren().containsKey("skysongstats.skill.mining.novice"));
    }

    @Test
    void unregisterAllRemovesThemAndLeavesOtherWildcardChildren() {
        wildcard.getChildren().put("skysongstats.reload", true);
        registry.register(SkillTestConfig.load());
        registry.unregisterAll();
        assertNull(server.getPluginManager().getPermission("skysongstats.skill.farming.novice"));
        assertEquals(Map.of("skysongstats.reload", true), wildcard.getChildren());
    }
}
