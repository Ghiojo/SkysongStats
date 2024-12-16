package org.skysongdev.skysongstats.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.inventories.PointAllocGUI;
import org.skysongdev.skysongstats.inventories.StatSetupGUI;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class StatlineGUIListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof StatSetupGUI) {
           if(!event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(selectableKey)){
                event.setCancelled(true);
                return;
           }
           SetupProfile setup = getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getWhoClicked().getUniqueId().toString());
           if(event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(statlineKey)){
               String key = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(statlineKey, PersistentDataType.STRING);
               int[] stat = {getPlugin().getConfig().getInt("base-stats." + key + ".strength"), 0};
               setup.setStrength(stat);
               stat[0] = getPlugin().getConfig().getInt("base-stats." + key + ".dexterity");
               setup.setDexterity(stat);
               stat[0] = getPlugin().getConfig().getInt("base-stats." + key + ".constitution");
               setup.setConstitution(stat);
               stat[0] = getPlugin().getConfig().getInt("base-stats." + key + ".focus");
               setup.setFocus(stat);
               stat[0] = getPlugin().getConfig().getInt("base-stats." + key + ".speed");
               setup.setSpeed(stat);
               setup.setupPointAllocGui();
               event.getWhoClicked().openInventory(setup.getPointAllocGUI().getInventory());
           }
        }
    }
}
