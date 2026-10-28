package org.skysongdev.skysongstats.Utils;

import org.bukkit.NamespacedKey;

// The item tags Skysong_Items writes that this plugin reads. They are built here
// instead of being taken from Skysong_Items' classes, so a refactor there cannot
// break this plugin at runtime. The names are stored on items already in
// circulation, so they only change if Skysong_Items changes them.
public final class SkysongItemsKeys {
    // Bukkit derives the namespace from Skysong_Items' plugin.yml name, lowercased.
    public static final String NAMESPACE = "skysong_items";

    public static final NamespacedKey ARCROT = new NamespacedKey(NAMESPACE, "arcrot");
    public static final NamespacedKey CHARGED_ARCROT = new NamespacedKey(NAMESPACE, "charged_arcrot");
    public static final NamespacedKey FICTIS = new NamespacedKey(NAMESPACE, "fictis");
    public static final NamespacedKey CHARGES = new NamespacedKey(NAMESPACE, "charges");

    private SkysongItemsKeys() {
    }
}
