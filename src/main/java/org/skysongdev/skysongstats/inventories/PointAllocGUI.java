package org.skysongdev.skysongstats.inventories;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;
import static org.skysongdev.skysongstats.Utils.Utils.StaticStats.*;

public class PointAllocGUI implements InventoryHolder {
    private final Inventory inventory;
    Map<StaticStats, Integer> statMap;

    ItemStack strengthItem = new ItemStack(Material.IRON_SWORD);
    ItemStack dexterityItem = new ItemStack(Material.BOW);
    ItemStack constitutionItem = new ItemStack(Material.APPLE);
    ItemStack focusItem = new ItemStack(Material.ENDER_EYE);
    ItemStack speedItem = new ItemStack(Material.FEATHER);

    ItemStack headerItem = new ItemStack(Material.NETHER_STAR);

    public int negativePoints = 3;
    public int points = 7;

    public PointAllocGUI() {
        this.inventory = getPlugin().getServer().createInventory(this, 27);
        this.statMap = new HashMap<>(Map.of(
                STRENGTH, 0,
                DEXTERITY, 0,
                CONSTITUTION, 0,
                FOCUS, 0,
                SPEED, 6
        ));
    }

    public void setupInventory() {
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for(int i = 0; i < 9; i++){
            this.inventory.setItem(i, borderItem);
        }
        for(int i = 18; i < 27; i++){
            this.inventory.setItem(i, borderItem);
        }
        updateStat(STRENGTH);
        updateStat(DEXTERITY);
        updateStat(CONSTITUTION);
        updateStat(FOCUS);
        updateStat(SPEED);

        ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<green>Confirm"));
        confirmMeta.getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        confirmMeta.getPersistentDataContainer().set(optionButtonKey, PersistentDataType.STRING, "confirm");
        confirmItem.setItemMeta(confirmMeta);
        this.inventory.setItem(22, confirmItem);
    }

    public void updateStat(StaticStats stat){
        List<Component> lore = new ArrayList<>();
        ItemMeta bufferMeta;
        switch(stat){
            case STRENGTH:
                bufferMeta = strengthItem.getItemMeta();
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<red><bold>Strength:</bold> " + statMap.get(STRENGTH)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + statMap.get(STRENGTH)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's Physical Strength"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Strength Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Regular Melee Attack Chances (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Regular Melee Damage (+1 per two points over 0)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - The types of Armor you can wear (some armors have STR requirements)"));
                bufferMeta.lore(lore);
                bufferMeta.getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                bufferMeta.getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "strength");
                strengthItem.setItemMeta(bufferMeta);
                lore.clear();
                this.inventory.setItem(9, strengthItem);
                break;
            case DEXTERITY:
                bufferMeta = dexterityItem.getItemMeta();
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<green><bold>Dexterity:</bold> " + statMap.get(DEXTERITY)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + statMap.get(DEXTERITY)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's Balance and Coordination"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Dexterity Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Ranged Attack Chances (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Finesse Weapon Melee Attack Chances (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - AC granted by an armor's Dexterity Cap"));
                bufferMeta.lore(lore);
                bufferMeta.getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                bufferMeta.getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "dexterity");
                dexterityItem.setItemMeta(bufferMeta);
                lore.clear();
                this.inventory.setItem(11, dexterityItem);
                break;
            case CONSTITUTION:
                bufferMeta = constitutionItem.getItemMeta();
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<light_purple><bold>Constitution:</bold> " + statMap.get(CONSTITUTION)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + statMap.get(CONSTITUTION)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's Health, Stamina and Resistance"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Constitution Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Health (+10HP per point over 0)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Potion Sickness Tolerance (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Saves against Wounds (+1 per point)"));
                bufferMeta.lore(lore);
                bufferMeta.getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                bufferMeta.getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "constitution");
                constitutionItem.setItemMeta(bufferMeta);
                lore.clear();
                this.inventory.setItem(13, constitutionItem);
                break;
            case StaticStats.FOCUS:
                bufferMeta = focusItem.getItemMeta();
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<blue><bold>Focus:</bold> " + statMap.get(FOCUS)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + statMap.get(FOCUS)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's attunement to the supernatural"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Focus Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Spellcasting DCs and Attack Rolls (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Perception checks on the supernatural (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Your Arcrot Tolerance (+2 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Your Fictis Tolerance (+1 per point)"));
                bufferMeta.lore(lore);
                bufferMeta.getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                bufferMeta.getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "focus");
                focusItem.setItemMeta(bufferMeta);
                lore.clear();
                this.inventory.setItem(15, focusItem);
                break;
            case StaticStats.SPEED:
                bufferMeta = speedItem.getItemMeta();
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<blue><bold>Speed:</bold> " + statMap.get(SPEED)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + statMap.get(SPEED)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's physical speed"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Speed Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Movement Speed (+1 block per point)"));
                bufferMeta.lore(lore);
                bufferMeta.getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                bufferMeta.getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "speed");
                speedItem.setItemMeta(bufferMeta);
                lore.clear();
                this.inventory.setItem(17, speedItem);
                break;
            default:
                break;
        }
        updateHeader();
    }

    private void updateHeader(){
        ItemMeta headerMeta = headerItem.getItemMeta();
        headerMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Stat Points Allocation"));
        List<Component> headerLore = new ArrayList<>();
        headerLore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Allocate your stat points to your character's base stats"));
        headerLore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>You can decrease a stat by 2 (or two stats by 1) once"));
        headerLore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>to gain 1 extra point to allocate"));
        headerLore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Points Remaining: <gold>" + points));
        headerLore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Negative Points: <red>" + negativePoints));
        headerMeta.lore(headerLore);
        headerItem.setItemMeta(headerMeta);
        this.inventory.setItem(4, headerItem);
    }

    public void addStat(StaticStats stat, int num){
        if (num == 0) return; // No change requested
        int currentStatValue = (stat != SPEED)? statMap.get(stat): statMap.get(stat) - 6; // Speed starts at 6, but is treated as 0 for allocation purposes

        if (num > 0) { // Adding points
            if (currentStatValue < 0) { // Transition from negative to positive
                if (points <= 0) return;
                statMap.put(stat, statMap.get(stat)+num);
                negativePoints += num;
            } else { // Regular addition
                if (points <= 0 || currentStatValue + num > 4) return;
                statMap.put(stat, statMap.get(stat)+num);
            }
            points -= num;
        } else { // Subtracting points
            if(currentStatValue <= -3) return;
            if(currentStatValue <= 0){
                if(negativePoints <= 0) return;
                statMap.put(stat, statMap.get(stat)+num);
                negativePoints += num;
            } else{
                statMap.put(stat, statMap.get(stat)+num);
            }
            points -= num;
        }
        updateStat(stat);

    }

    public void returnFinalizedStats(SetupProfile playerProfile){
        playerProfile.setStrength(statMap.get(STRENGTH));
        playerProfile.setDexterity(statMap.get(DEXTERITY));
        playerProfile.setConstitution(statMap.get(CONSTITUTION));
        playerProfile.setFocus(statMap.get(FOCUS));
        playerProfile.setSpeed(statMap.get(SPEED));
    }

    public void resetPoints(){
        points = 7;
        negativePoints = 3;
        statMap.put(STRENGTH, 0);
        statMap.put(DEXTERITY, 0);
        statMap.put(CONSTITUTION, 0);
        statMap.put(FOCUS, 0);
        statMap.put(SPEED, 6);
        updateStat(StaticStats.ARCROT_MAX);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
