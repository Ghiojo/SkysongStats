package org.skysongdev.skysongstats.inventories;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;
import static org.skysongdev.skysongstats.Utils.Utils.StaticStats.*;

public class PointAllocGUI implements InventoryHolder {
    private final Inventory inventory;
    public int str;
    public int dex;
    public int con;
    public int foc;
    public int spd;

    SetupProfile playerProfile;
    ItemStack strengthItem = new ItemStack(Material.IRON_SWORD);
    ItemStack dexterityItem = new ItemStack(Material.BOW);
    ItemStack constitutionItem = new ItemStack(Material.APPLE);
    ItemStack focusItem = new ItemStack(Material.ENDER_EYE);
    ItemStack speedItem = new ItemStack(Material.FEATHER);

    ItemStack headerItem = new ItemStack(Material.NETHER_STAR);

    public int negativePoints = 2;
    public int points = 5;

    public PointAllocGUI() {
        this.inventory = getPlugin().getServer().createInventory(this, 27);
        this.str = 0;
        this.dex = 0;
        this.con = 0;
        this.foc = 0;
        this.spd = 0;
    }

    public void setupInventory(SetupProfile playerProfile) {
        this.playerProfile = playerProfile;

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
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<red><bold>Strength:</bold> " + (playerProfile.getStrength()[0] + str)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + str));
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
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<green><bold>Dexterity:</bold> " + (playerProfile.getDexterity()[0] + dex)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + dex));
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
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<light_purple><bold>Constitution:</bold> " + ( playerProfile.getConstitution()[0] + con)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + con));
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
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<blue><bold>Focus:</bold> " + ( playerProfile.getFocus()[0] + foc)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + foc));
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
                bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<blue><bold>Speed:</bold> " + ( playerProfile.getSpeed()[0] + spd)));
                lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Points Allocated: " + spd));
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
        switch(stat){
            case STRENGTH:
                if (num > 0) {
                    if(str < 0){
                        if(negativePoints <= 0){
                            if(points <= 1){
                                return;
                            }
                            if(str == 3 || dex == 3 || con == 3 || foc == 3 || spd == 3){
                                return;
                            }
                            points--;
                        }
                        negativePoints += num;
                        str += num;
                    }else {
                        if(points <= 0){
                            return;
                        }
                        if (str + num > 2 && negativePoints > 0) {
                            return;
                        }
                        if(str + num > 3){
                            return;
                        }
                        str += num;
                        points -= num;
                    }
                } else {
                    if (str + num < 0) {
                        if (negativePoints <= 0) {
                            return;
                        }
                        str += num;
                        negativePoints += num; // num is negative, so this subtracts
                        if(negativePoints <= 0){
                            points++;
                        }
                    } else {
                        str += num;
                        points -= num; // num is negative, so this adds
                    }
                }
                updateStat(STRENGTH);
                break;
            case DEXTERITY:
                if (num > 0) {
                    if(dex < 0){
                        if(negativePoints <= 0){
                            if(points <= 1){
                                return;
                            }
                            if(str == 3 || dex == 3 || con == 3 || foc == 3 || spd == 3){
                                return;
                            }
                            points--;
                        }
                        negativePoints += num;
                        dex += num;
                    }else {
                        if(points <= 0){
                            return;
                        }
                        if (dex + num > 2 && negativePoints > 0) {
                            return;
                        }
                        if(dex + num > 3){
                            return;
                        }
                        dex += num;
                        points -= num;
                    }
                } else {
                    if (dex + num < 0) {
                        if (negativePoints <= 0) {
                            return;
                        }
                        dex += num;
                        negativePoints += num; // num is negative, so this subtracts
                        if(negativePoints <= 0){
                            points++;
                        }
                    } else {
                        dex += num;
                        points -= num; // num is negative, so this adds
                    }
                }
                updateStat(DEXTERITY);
                break;
            case CONSTITUTION:
                if (num > 0) {
                    if(con < 0){
                        if(negativePoints <= 0){
                            if(points <= 1){
                                return;
                            }
                            if(str == 3 || dex == 3 || con == 3 || foc == 3 || spd == 3){
                                return;
                            }
                            points--;
                        }
                        negativePoints += num;
                        con += num;
                    }else {
                        if(points <= 0){
                            return;
                        }
                        if (con + num > 2 && negativePoints > 0) {
                            return;
                        }
                        if(con + num > 3){
                            return;
                        }
                        con += num;
                        points -= num;
                    }
                } else {
                    if (con + num < 0) {
                        if (negativePoints <= 0) {
                            return;
                        }
                        con += num;
                        negativePoints += num; // num is negative, so this subtracts
                        if(negativePoints <= 0){
                            points++;
                        }
                    } else {
                        con += num;
                        points -= num; // num is negative, so this adds
                    }
                }
                updateStat(CONSTITUTION);
                break;
            case FOCUS:
                if (num > 0) {
                    if(foc < 0){
                        if(negativePoints <= 0){
                            if(points <= 1){
                                return;
                            }
                            if(str == 3 || dex == 3 || con == 3 || foc == 3 || spd == 3){
                                return;
                            }
                            points--;
                        }
                        negativePoints += num;
                        foc += num;
                    }else {
                        if(points <= 0){
                            return;
                        }
                        if (foc + num > 2 && negativePoints > 0) {
                            return;
                        }
                        if(foc + num > 3){
                            return;
                        }
                        foc += num;
                        points -= num;
                    }
                } else {
                    if (foc + num < 0) {
                        if (negativePoints <= 0) {
                            return;
                        }
                        foc += num;
                        negativePoints += num; // num is negative, so this subtracts
                        if(negativePoints <= 0){
                            points++;
                        }
                    } else {
                        foc += num;
                        points -= num; // num is negative, so this adds
                    }
                }
                updateStat(StaticStats.FOCUS);
                break;
            case SPEED:
                if (num > 0) {
                    if(spd < 0){
                        if(negativePoints <= 0){
                            if(points <= 1){
                                return;
                            }
                            if(str == 3 || dex == 3 || con == 3 || foc == 3 || spd == 3){
                                return;
                            }
                            points--;
                        }
                        negativePoints += num;
                        spd += num;
                    }else {
                        if(points <= 0){
                            return;
                        }
                        if (spd + num > 2 && negativePoints > 0) {
                            return;
                        }
                        if(spd + num > 3){
                            return;
                        }
                        spd += num;
                        points -= num;
                    }
                } else {
                    if (spd + num < 0) {
                        if (negativePoints <= 0) {
                            return;
                        }
                        spd += num;
                        negativePoints += num; // num is negative, so this subtracts
                        if(negativePoints <= 0){
                            points++;
                        }
                    } else {
                        spd += num;
                        points -= num; // num is negative, so this adds
                    }
                }
                updateStat(StaticStats.SPEED);
                break;
            default:
        }
    }

    public void returnFinalizedStats(SetupProfile playerProfile){
        playerProfile.setStrength(new int[]{playerProfile.getStrength()[0], str});
        playerProfile.setDexterity(new int[]{playerProfile.getDexterity()[0], dex});
        playerProfile.setConstitution(new int[]{playerProfile.getConstitution()[0], con});
        playerProfile.setFocus(new int[]{playerProfile.getFocus()[0], foc});
        playerProfile.setSpeed(new int[]{playerProfile.getSpeed()[0], spd});
    }

    public void resetPoints(){
        points = 5;
        negativePoints = 2;
        str = 0;
        dex = 0;
        con = 0;
        foc = 0;
        spd = 0;
        updateStat(StaticStats.ARCROT_MAX);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
