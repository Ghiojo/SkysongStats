package org.skysongdev.skysongstats.proficiency;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.permissions.GeneratedPermissions;

import java.util.ArrayList;
import java.util.List;

// Declares the proficiency tier nodes generated from proficiencies.yml (see
// ProficiencyPermissions) to the server, under the skysongstats.* wildcard, so
// they show in LuckPerms' editor. Ops have them by default, like the skill level
// nodes. A reload swaps them.
public final class ProficiencyPermissionRegistry {
    private final GeneratedPermissions permissions;

    public ProficiencyPermissionRegistry(PluginManager plugins) {
        this.permissions = new GeneratedPermissions(plugins, ProficiencyPermissions.PREFIX);
    }

    public void register(ProficienciesConfig config) {
        List<Permission> nodes = new ArrayList<>();
        for (ProficiencyDefinition proficiency : config.proficiencies().all()) {
            config.tiers().all().stream().skip(1).forEach(tier -> nodes.add(new Permission(
                    ProficiencyPermissions.node(proficiency, tier),
                    "Given to characters at " + tier.name() + " " + proficiency.name() + " or higher",
                    PermissionDefault.OP)));
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
