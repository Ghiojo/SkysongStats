package org.skysongdev.skysongstats.setup;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

// Passes clicks on an open /setup screen to it. Nothing can be taken out of or
// put into the screen.
public final class SetupScreenListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SetupScreen screen)) {
            return;
        }
        event.setCancelled(true);
        boolean inScreen = event.getRawSlot() >= 0 && event.getRawSlot() < screen.size();
        if (inScreen && event.getWhoClicked() instanceof Player player) {
            screen.click(player, event.getRawSlot(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SetupScreen) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SetupScreen screen) {
            screen.closed();
        }
    }
}
