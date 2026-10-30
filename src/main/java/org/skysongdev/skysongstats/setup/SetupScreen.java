package org.skysongdev.skysongstats.setup;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;

// A /setup screen (the stat points, the skill picks): an inventory nothing can be
// taken out of or put into, which handles clicks on its own slots.
public interface SetupScreen extends InventoryHolder {
    // The number of slots of the screen's own inventory.
    int size();

    // A click on one of the screen's slots.
    void click(Player player, int slot, ClickType click);

    // The player closed the screen.
    void closed();
}
