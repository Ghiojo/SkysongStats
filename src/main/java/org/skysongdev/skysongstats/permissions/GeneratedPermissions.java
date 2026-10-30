package org.skysongdev.skysongstats.permissions;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Permission nodes the plugin builds from a config file (stats.yml, skills.yml)
// instead of listing them in plugin.yml. They are declared to the server, so
// permission plugins can list them, and sit under the skysongstats.* wildcard
// like the plugin.yml nodes. replace() swaps them all, for /ssreload.
public final class GeneratedPermissions {
    public static final String WILDCARD = "skysongstats.*";

    private final PluginManager plugins;
    private final String prefix;
    private final List<Permission> registered = new ArrayList<>();

    // prefix is what every generated node starts with, e.g. "skysongstats.stat.".
    public GeneratedPermissions(PluginManager plugins, String prefix) {
        this.plugins = Objects.requireNonNull(plugins, "plugins");
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    public void replace(List<Permission> permissions) {
        removeAll();
        for (Permission permission : permissions) {
            plugins.addPermission(permission);
            registered.add(permission);
        }
        updateWildcard();
    }

    public void clear() {
        removeAll();
        updateWildcard();
    }

    public List<String> names() {
        return registered.stream().map(Permission::getName).toList();
    }

    private void removeAll() {
        registered.forEach(plugins::removePermission);
        registered.clear();
    }

    private void updateWildcard() {
        Permission wildcard = plugins.getPermission(WILDCARD);
        if (wildcard == null) {
            return;
        }
        wildcard.getChildren().keySet().removeIf(name -> name.startsWith(prefix));
        registered.forEach(permission -> wildcard.getChildren().put(permission.getName(), true));
        wildcard.recalculatePermissibles();
    }
}
