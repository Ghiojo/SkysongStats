package org.skysongdev.skysongstats.listeners;


import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.inventories.SkillChoiceGUI;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class SkillAllocGUIListener implements Listener {
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        if(event.getInventory().getHolder() instanceof SkillChoiceGUI){
            SkillChoiceGUI gui = (SkillChoiceGUI) event.getInventory().getHolder();
            gui.resetSelectedSkills();
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof SkillChoiceGUI){
            if(event.getCurrentItem() == null){
                return;
            }
            PersistentDataContainer itemData = event.getCurrentItem().getItemMeta().getPersistentDataContainer();
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if(!itemData.has(selectableKey)){
                return;
            }
            SetupProfile setup = getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getWhoClicked().getUniqueId().toString());
            SkillChoiceGUI gui = setup.getSkillChoiceGUI();
            if(itemData.has(skillButtonKey)){
                if(!event.getClick().isLeftClick() && !event.getClick().isRightClick()){
                    event.setCancelled(true);
                    return;
                }
                int skill = itemData.get(skillButtonKey, PersistentDataType.INTEGER);
                ItemStack item = event.getCurrentItem();
                ItemMeta meta = item.getItemMeta();
                if(event.getClick().isLeftClick())
                    meta.setEnchantmentGlintOverride(gui.setSelectedSkills(skill));
                else
                    meta.setEnchantmentGlintOverride(!gui.removeSelectedSkills(skill));
                item.setItemMeta(meta);
                event.getWhoClicked().sendMessage("Skills: " + setup.skillsSelected.toString());
                gui.updateHeader();
            }
            if(itemData.has(optionButtonKey)){
                if(itemData.get(optionButtonKey, PersistentDataType.STRING).equals("confirm")){
                    setup.setSkillsSelected(setup.skillsSelected);
                    setup.finishProfileSetup();
                    event.getWhoClicked().closeInventory();
                    event.getWhoClicked().sendMessage(getPlugin().getUtils().getMiniMessage().deserialize(getPlugin().getUtils().PLUGIN_TAG + "<green>Your Setup have been Completed!"));
                }
            }
        }

    }
}
