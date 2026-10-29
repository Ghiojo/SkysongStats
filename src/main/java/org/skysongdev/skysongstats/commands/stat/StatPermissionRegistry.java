package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// Declares the permission nodes generated for stats.yml (see StatPermissions) to
// the server, so permission plugins can list them and they sit under the
// skysongstats.* wildcard like the nodes in plugin.yml. Who has them by default is
// the stat's permission-default (ops, unless stats.yml says otherwise).
// A reload swaps them for the new file's nodes.
public final class StatPermissionRegistry {
    static final String WILDCARD = "skysongstats.*";

    private final PluginManager plugins;
    private final List<Permission> registered = new ArrayList<>();

    public StatPermissionRegistry(PluginManager plugins) {
        this.plugins = Objects.requireNonNull(plugins, "plugins");
    }

    public void register(StatDefinitions definitions) {
        unregisterAll();
        for (StatDefinition stat : definitions.all()) {
            actions(stat).forEach(action -> {
                add(StatPermissions.self(stat, action),
                        "Use " + action.configName() + " on your own " + stat.display().name(), stat);
                add(StatPermissions.other(stat, action),
                        "Use " + action.configName() + " on other players' " + stat.display().name(), stat);
            });
        }
        updateWildcard();
    }

    public void unregisterAll() {
        registered.forEach(plugins::removePermission);
        registered.clear();
        updateWildcard();
    }

    List<String> names() {
        return registered.stream().map(Permission::getName).toList();
    }

    // Damage only exists for bars with a shield.
    private static Stream<StatAction> actions(StatDefinition stat) {
        boolean shield = stat.isBar() && stat.bar().shield() != null;
        return Arrays.stream(StatAction.values()).filter(action -> action != StatAction.DAMAGE || shield);
    }

    private void add(String name, String description, StatDefinition stat) {
        Permission permission = new Permission(name, description, stat.permissionDefault());
        plugins.addPermission(permission);
        registered.add(permission);
    }

    private void updateWildcard() {
        Permission wildcard = plugins.getPermission(WILDCARD);
        if (wildcard == null) {
            return;
        }
        wildcard.getChildren().keySet().removeIf(name -> name.startsWith(StatPermissions.PREFIX));
        registered.forEach(permission -> wildcard.getChildren().put(permission.getName(), true));
        wildcard.recalculatePermissibles();
    }
}
