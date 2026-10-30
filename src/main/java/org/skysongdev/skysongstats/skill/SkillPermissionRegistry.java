package org.skysongdev.skysongstats.skill;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongstats.definition.SkillDefinition;
import org.skysongdev.skysongstats.definition.SkillLevel;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.permissions.GeneratedPermissions;

import java.util.ArrayList;
import java.util.List;

// Declares the skill level nodes generated from skills.yml (see SkillPermissions)
// to the server, under the skysongstats.* wildcard. Ops have them by default, as
// with the hand-written list plugin.yml had. A reload swaps them.
public final class SkillPermissionRegistry {
    private final GeneratedPermissions permissions;

    public SkillPermissionRegistry(PluginManager plugins) {
        this.permissions = new GeneratedPermissions(plugins, SkillPermissions.PREFIX);
    }

    public void register(SkillsConfig config) {
        List<Permission> nodes = new ArrayList<>();
        List<SkillLevel> levels = config.levels().all();
        for (SkillDefinition skill : config.skills().all()) {
            levels.stream().skip(1).forEach(level -> nodes.add(new Permission(SkillPermissions.node(skill, level),
                    "Given to characters at " + level.name() + " " + skill.name() + " or higher", PermissionDefault.OP)));
        }
        permissions.replace(nodes);
    }

    public void unregisterAll() {
        permissions.clear();
    }

    List<String> names() {
        return permissions.names();
    }
}
