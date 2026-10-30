package org.skysongdev.skysongstats.setup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.skysongdev.skysongstats.definition.Icon;

import java.util.Optional;

// Item and text helpers shared by the /setup screens.
final class MenuItems {
    static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MenuItems() {
    }

    // The item for an icon from a definition file: its Material, or paper with
    // the item model.
    static ItemStack icon(Icon icon) {
        if (!icon.isModel()) {
            return new ItemStack(Optional.ofNullable(Material.matchMaterial(icon.material())).orElse(Material.BARRIER));
        }
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(icon.model())));
        return item;
    }

    // The text in a MiniMessage colour ("<red>"), for the parts that set none.
    static Component colored(String color, Component text) {
        return MINI_MESSAGE.deserialize(color + "<text>", Placeholder.component("text", text));
    }
}
