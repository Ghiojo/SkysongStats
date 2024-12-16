package org.skysongdev.skysongstats.inventories;


import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongstats.database.SetupProfile;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class SkillAllocGUIListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof SkillChoiceGUI){
            PersistentDataContainer itemData = event.getCurrentItem().getItemMeta().getPersistentDataContainer();
            if(!itemData.has(selectableKey)){
                event.setCancelled(true);
                return;
            }
            SkillChoiceGUI gui = (SkillChoiceGUI) event.getInventory().getHolder();
            SetupProfile setup = getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getWhoClicked().getUniqueId().toString());
            if(itemData.has(skillButtonKey)){
                if(!event.getClick().isLeftClick() && !event.getClick().isRightClick()){
                    event.setCancelled(true);
                    return;
                }
                int skill = itemData.get(skillButtonKey, PersistentDataType.INTEGER);
                ItemStack item = event.getCurrentItem();
                if(event.getClick().isLeftClick())
                    item.getItemMeta().setEnchantmentGlintOverride(gui.setSelectedSkills(skill));
                else
                    item.getItemMeta().setEnchantmentGlintOverride(gui.removeSelectedSkills(skill));
            }
            if(itemData.has(optionButtonKey)){
                if(itemData.get(optionButtonKey, PersistentDataType.STRING).equals("confirm")){
                    setup.setSkillsSelected(gui.getSelectedSkills());
                    event.getWhoClicked().closeInventory();
                    event.getWhoClicked().sendMessage(getPlugin().getUtils().getMiniMessage().deserialize(getPlugin().getUtils().PLUGIN_TAG + "<green>Your Setup have been Completed!"));
                }
            }
        }

    }
}
