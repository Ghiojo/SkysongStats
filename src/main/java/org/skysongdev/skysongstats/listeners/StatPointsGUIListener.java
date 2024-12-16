package org.skysongdev.skysongstats.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.inventories.PointAllocGUI;

import java.net.http.WebSocket;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class StatPointsGUIListener implements WebSocket.Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof PointAllocGUI){
            PersistentDataContainer itemData = event.getCurrentItem().getItemMeta().getPersistentDataContainer();
            if(!itemData.has(selectableKey)){
                event.setCancelled(true);
                return;
            }
            PointAllocGUI gui = (PointAllocGUI) event.getInventory().getHolder();
            SetupProfile setup = getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getWhoClicked().getUniqueId().toString());
            if(itemData.has(statButtonKey)){
                if(!event.getClick().isLeftClick() && !event.getClick().isRightClick()){
                    event.setCancelled(true);
                    return;
                }
                boolean isPlus = event.getClick().isLeftClick();
                switch(itemData.get(statButtonKey, PersistentDataType.STRING)){
                    case "strength":
                        gui.addStat(Utils.StaticStats.STRENGTH, isPlus?1:-1);
                        gui.updateStat(Utils.StaticStats.STRENGTH);
                        break;
                    case "dexterity":
                        gui.addStat(Utils.StaticStats.DEXTERITY, isPlus?1:-1);
                        gui.updateStat(Utils.StaticStats.DEXTERITY);
                        break;
                    case "constitution":
                        gui.addStat(Utils.StaticStats.CONSTITUTION, isPlus?1:-1);
                        gui.updateStat(Utils.StaticStats.CONSTITUTION);
                        break;
                    case "focus":
                        gui.addStat(Utils.StaticStats.FOCUS, isPlus?1:-1);
                        gui.updateStat(Utils.StaticStats.FOCUS);
                        break;
                    case "speed":
                        gui.addStat(Utils.StaticStats.SPEED, isPlus?1:-1);
                        gui.updateStat(Utils.StaticStats.SPEED);
                        break;
                }
            }
            if(itemData.has(optionButtonKey)){
                if(itemData.get(optionButtonKey, PersistentDataType.STRING).equals("confirm")){
                    gui.returnFinalizedStats(setup);
                    event.getWhoClicked().openInventory(setup.getSkillChoiceGUI().getInventory());
                }
            }
        }
    }
}
