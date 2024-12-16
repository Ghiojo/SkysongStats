package org.skysongdev.skysongstats.inventories;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class PointAllocGUI implements InventoryHolder {
    private final Inventory inventory;
    private int[] str = new int[2];
    private int[] dex = new int[2];
    private int[] con = new int[2];
    private int[] foc = new int[2];
    private int[] spd = new int[2];

    ItemStack strengthItem = new ItemStack(Material.IRON_SWORD);
    ItemStack dexterityItem = new ItemStack(Material.BOW);
    ItemStack constitutionItem = new ItemStack(Material.APPLE);
    ItemStack focusItem = new ItemStack(Material.ENDER_EYE);
    ItemStack speedItem = new ItemStack(Material.FEATHER);

    private int negativePoints = 0;
    private int points = 5;

    public PointAllocGUI() {
        this.inventory = getPlugin().getServer().createInventory(this, 27);;
    }

    public void setupInventory(SetupProfile playerProfile) {
        str = playerProfile.getStrength();
        dex = playerProfile.getDexterity();
        con = playerProfile.getConstitution();
        foc = playerProfile.getFocus();
        spd = playerProfile.getSpeed();

        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for(int i = 0; i < 9; i++){
            this.inventory.setItem(i, borderItem);
        }
        for(int i = 18; i < 27; i++){
            this.inventory.setItem(i, borderItem);
        }
        updateStat(StaticStats.ARCROT_MAX);

        ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        confirmItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<green>Confirm"));
        confirmItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        confirmItem.getItemMeta().getPersistentDataContainer().set(optionButtonKey, PersistentDataType.STRING, "confirm");
        this.inventory.setItem(22, confirmItem);
    }

    public void updateStat(StaticStats stat){
        List<Component> lore = new ArrayList<>();
        switch(stat){
            case StaticStats.STRENGTH:
                strengthItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<red><bold>Strength:</bold> " + ( str[0] + str[1])));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + str[1]));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's Physical Strength"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Strength Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Regular Melee Attack Chances (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Regular Melee Damage (+1 per two points over 0)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - The types of Armor you can wear (some armors have STR requirements)"));
                strengthItem.lore(lore);
                strengthItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                strengthItem.getItemMeta().getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "strength");
                this.inventory.setItem(10, strengthItem);
                break;
            case StaticStats.DEXTERITY:
                dexterityItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<green><bold>Dexterity:</bold> " + (dex[0] + dex[1])));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + dex[1]));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's Balance and Coordination"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Dexterity Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Ranged Attack Chances (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Finesse Weapon Melee Attack Chances (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - AC granted by an armor's Dexterity Cap"));
                dexterityItem.lore(lore);
                dexterityItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                dexterityItem.getItemMeta().getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "dexterity");
                lore.clear();
                this.inventory.setItem(12, dexterityItem);
                break;
            case StaticStats.CONSTITUTION:
                constitutionItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<light_purple><bold>Constitution:</bold> " + ( con[0] + con[1])));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + con[1]));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's Health, Stamina and Resistance"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Constitution Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Health (+10HP per point over 0)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Potion Sickness Tolerance (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Saves against Wounds (+1 per point)"));
                constitutionItem.lore(lore);
                constitutionItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                constitutionItem.getItemMeta().getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "constitution");
                lore.clear();
                this.inventory.setItem(14, constitutionItem);
                break;
            case StaticStats.FOCUS:
                focusItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<blue><bold>Focus:</bold> " + ( foc[0] + foc[1])));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + foc[1]));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's attunement to the supernatural"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Focus Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Spellcasting DCs and Attack Rolls (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Perception checks on the supernatural (+1 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Your Arcrot Tolerance (+2 per point)"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Your Fictis Tolerance (+1 per point)"));
                focusItem.lore(lore);
                focusItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                focusItem.getItemMeta().getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "focus");
                lore.clear();
                this.inventory.setItem(16, focusItem);
                break;
            case StaticStats.SPEED:
                speedItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<blue><bold>Speed:</bold> " + ( spd[0] + spd[1])));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + spd[1]));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Left Click to Increase by 1, Right Click to Decrease by 1"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Determines your character's physical speed"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Speed Affects:"));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray> - Movement Speed (+1 block per point)"));
                speedItem.lore(lore);
                speedItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
                speedItem.getItemMeta().getPersistentDataContainer().set(statButtonKey, PersistentDataType.STRING, "speed");
                lore.clear();
                this.inventory.setItem(16, speedItem);
                break;
            default:
                updateStat(StaticStats.STRENGTH);
                updateStat(StaticStats.DEXTERITY);
                updateStat(StaticStats.CONSTITUTION);
                updateStat(StaticStats.FOCUS);
                updateStat(StaticStats.SPEED);
        }
    }

    public boolean addStat(StaticStats stat, int num){
        switch(stat){
            case StaticStats.STRENGTH:
                str[1] += num;
                break;
            case StaticStats.DEXTERITY:
                dex[1] += num;
                break;
            case StaticStats.CONSTITUTION:
                con[1] += num;
                break;
            case StaticStats.FOCUS:
                foc[1] += num;
                break;
            case StaticStats.SPEED:
                spd[1] += num;
                break;
        }
        return true;
    }

    public void returnFinalizedStats(SetupProfile playerProfile){
        playerProfile.setStrength(str);
        playerProfile.setDexterity(dex);
        playerProfile.setConstitution(con);
        playerProfile.setFocus(foc);
        playerProfile.setSpeed(spd);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
