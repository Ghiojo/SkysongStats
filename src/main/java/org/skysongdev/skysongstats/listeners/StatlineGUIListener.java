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
           event.setCancelled(true);
            if(event.getCurrentItem() == null){
                return;
            }
           if(!event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(selectableKey)){
                return;
           }
           SetupProfile setup = getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getWhoClicked().getUniqueId().toString());
           if(event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(statlineKey)){
               String key = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(statlineKey, PersistentDataType.STRING);
               setup.setStrength(new int[]{getPlugin().getConfig().getInt("base_stats." + key + ".stats.strength"), 0});
               setup.setDexterity(new int[]{getPlugin().getConfig().getInt("base_stats." + key + ".stats.dexterity"), 0});
               setup.setConstitution(new int[]{getPlugin().getConfig().getInt("base_stats." + key + ".stats.constitution"), 0});
               setup.setFocus(new int[]{getPlugin().getConfig().getInt("base_stats." + key + ".stats.focus"), 0});
               setup.setSpeed(new int[]{getPlugin().getConfig().getInt("base_stats." + key + ".stats.speed"), 0});
               setup.setupPointAllocGui();
               event.getWhoClicked().closeInventory();
               event.getWhoClicked().openInventory(setup.getPointAllocGUI().getInventory());
           }
        }
    }
}
