package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.permissions.GeneratedPermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

// Declares the permission nodes generated for stats.yml (see StatPermissions) to
// the server, under the skysongstats.* wildcard. Who has them by default is the
// stat's permission-default (ops, unless stats.yml says otherwise). A reload swaps
// them for the new file's nodes.
public final class StatPermissionRegistry {
    static final String WILDCARD = GeneratedPermissions.WILDCARD;

    private final GeneratedPermissions permissions;

    public StatPermissionRegistry(PluginManager plugins) {
        this.permissions = new GeneratedPermissions(plugins, StatPermissions.PREFIX);
    }

    public void register(StatDefinitions definitions) {
        List<Permission> nodes = new ArrayList<>();
        for (StatDefinition stat : definitions.all()) {
            actions(stat).forEach(action -> {
                nodes.add(new Permission(StatPermissions.self(stat, action),
                        "Use " + action.configName() + " on your own " + stat.display().name(), stat.permissionDefault()));
                nodes.add(new Permission(StatPermissions.other(stat, action),
                        "Use " + action.configName() + " on other players' " + stat.display().name(),
                        stat.permissionDefault()));
            });
        }
        permissions.replace(nodes);
    }

    public void unregisterAll() {
        permissions.clear();
    }

    List<String> names() {
        return permissions.names();
    }

    // Damage only exists for bars with a shield.
    private static Stream<StatAction> actions(StatDefinition stat) {
        boolean shield = stat.isBar() && stat.bar().shield() != null;
        return Arrays.stream(StatAction.values()).filter(action -> action != StatAction.DAMAGE || shield);
    }
}
