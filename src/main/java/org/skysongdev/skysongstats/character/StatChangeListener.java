package org.skysongdev.skysongstats.character;

import org.skysongdev.skysongstats.storage.ProfileKey;

// Told about every stat change StatService makes; the plugin turns these into
// Bukkit events. Not called for item counts, which are recounted, not changed.
@FunctionalInterface
public interface StatChangeListener {
    void onChange(StatChange change);

    // Called after anything that may have changed a character's totals: a stat
    // action, a modifier added or removed, or new item counts. Unlike onChange it
    // also covers modifiers and item counts, for things that follow a total, such
    // as the player's scale following their height.
    default void onTotalsChanged(ProfileKey character) {
    }
}
