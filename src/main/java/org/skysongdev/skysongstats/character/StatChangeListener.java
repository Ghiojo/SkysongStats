package org.skysongdev.skysongstats.character;

// Told about every stat change StatService makes; the plugin turns these into
// Bukkit events. Not called for item counts, which are recounted, not changed.
@FunctionalInterface
public interface StatChangeListener {
    void onChange(StatChange change);
}
