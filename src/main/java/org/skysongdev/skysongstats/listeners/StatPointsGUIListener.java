package org.skysongdev.skysongstats.listeners;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.inventories.PointAllocGUI;

import java.util.Arrays;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;
import static org.skysongdev.skysongstats.Utils.Utils.StaticStats.*;

public class StatPointsGUIListener implements Listener {
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        if(event.getInventory().getHolder() instanceof PointAllocGUI){
            PointAllocGUI gui = (PointAllocGUI) event.getInventory().getHolder();
            gui.resetPoints();
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof PointAllocGUI){
            event.setCancelled(true);
            if(event.getCurrentItem() == null){
                return;
            }
            PersistentDataContainer itemData = event.getCurrentItem().getItemMeta().getPersistentDataContainer();
            if(!itemData.has(selectableKey)){
                return;
            }
            PointAllocGUI gui = (PointAllocGUI) event.getInventory().getHolder();
            SetupProfile setup = getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getWhoClicked().getUniqueId().toString());
            if(itemData.has(statButtonKey)){
                if(!event.getClick().isLeftClick() && !event.getClick().isRightClick()){
                    return;
                }
                boolean isPlus = event.getClick().isLeftClick();
                String statKey = itemData.get(statButtonKey, PersistentDataType.STRING);
                if(statKey != null) {
                    switch (statKey) {
                        case "strength":
                            gui.addStat(STRENGTH, isPlus ? 1 : -1);
                            gui.updateStat(STRENGTH);
                            break;
                        case "dexterity":
                            gui.addStat(DEXTERITY, isPlus ? 1 : -1);
                            gui.updateStat(DEXTERITY);
                            break;
                        case "constitution":
                            gui.addStat(CONSTITUTION, isPlus ? 1 : -1);
                            gui.updateStat(CONSTITUTION);
                            break;
                        case "focus":
                            gui.addStat(FOCUS, isPlus ? 1 : -1);
                            gui.updateStat(FOCUS);
                            break;
                        case "speed":
                            gui.addStat(SPEED, isPlus ? 1 : -1);
                            gui.updateStat(SPEED);
                            break;
                        default:
                            break;
                    }
                }
                //event.getWhoClicked().sendMessage(gui.str + " | " + gui.dex + " | " + gui.con + " | " + gui.foc + " | " + gui.spd);
            }
            if(itemData.has(optionButtonKey)){
                if(itemData.get(optionButtonKey, PersistentDataType.STRING).equals("confirm")){
                    if(gui.points > 0 || gui.negativePoints == 1){
                        event.getWhoClicked().sendMessage(getPlugin().getUtils().getMiniMessage().deserialize(getPlugin().getUtils().PLUGIN_TAG + "<red>You still have points to allocate (Or have one negative point set)!"));
                        return;
                    }
                    gui.returnFinalizedStats(setup);
                    setup.setupSkillChoiceGui();
                    event.getWhoClicked().closeInventory();
                    event.getWhoClicked().openInventory(setup.getSkillChoiceGUI().getInventory());
                }
            }
        }
    }
}
