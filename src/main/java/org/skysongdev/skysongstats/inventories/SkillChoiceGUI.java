package org.skysongdev.skysongstats.inventories;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class SkillChoiceGUI implements InventoryHolder {
    Inventory inventory;

    public int[] getSelectedSkills() {
        int[] result = new int[2];
        result[0] = selectedSkills[0].getValue();
        result[1] = selectedSkills[1].getValue();
        return result;
    }

    Utils.Skills[] selectedSkills = new Utils.Skills[2];
    Player targetPlayer;

    int skillPoints = 2;

    ItemStack animalHandlingItem = new ItemStack(Material.BONE);
    ItemStack farmingItem = new ItemStack(Material.WHEAT);
    ItemStack forestryItem = new ItemStack(Material.FERN);
    ItemStack miningItem = new ItemStack(Material.IRON_ORE);
    ItemStack mistGatheringItem = new ItemStack(Material.GLOWSTONE_DUST);
    ItemStack woodProcessingItem = new ItemStack(Material.OAK_LOG);
    ItemStack alchemyItem = new ItemStack(Material.POTION);
    ItemStack artificingItem = new ItemStack(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
    ItemStack cookingItem = new ItemStack(Material.COOKED_BEEF);
    ItemStack craftsmanItem = new ItemStack(Material.ANVIL);
    ItemStack metalworkingItem = new ItemStack(Material.IRON_INGOT);
    ItemStack tailoringItem = new ItemStack(Material.FLOWER_BANNER_PATTERN);
    ItemStack witchcraftItem = new ItemStack(Material.ENCHANTING_TABLE);
    ItemStack economicalItem = new ItemStack(Material.GOLD_NUGGET);
    ItemStack scholarlyItem = new ItemStack(Material.BOOK);
    ItemStack entertainmentItem = new ItemStack(Material.MUSIC_DISC_CAT);
    ItemStack martialItem = new ItemStack(Material.IRON_SWORD);
    ItemStack medicalItem = new ItemStack(Material.PAPER);
    ItemStack tandeItem = new ItemStack(Material.COMPASS);



    public SkillChoiceGUI(){
        this.inventory = getPlugin().getServer().createInventory(this, 54);
    }

    public void setupInventory(Player player){
        targetPlayer = player;
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        List<Component> lore = new ArrayList<>();
        for(int i = 0; i < 9; i++){
            this.inventory.setItem(i, borderItem);
        }
        for(int i = 45; i < 54; i++){
            this.inventory.setItem(i, borderItem);
        }

        animalHandlingItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<dark_green><bold>Animal Handling</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to interact with animals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on animal trainers, animal farmers, or horse breeders, for example."));
        animalHandlingItem.lore(lore);
        animalHandlingItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        animalHandlingItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 0);
        lore.clear();
        this.inventory.setItem(10, animalHandlingItem);

        farmingItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gold><bold>Farming</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to tend to crops and plants."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on farmers, gardeners or orchard farmers, for example."));
        farmingItem.lore(lore);
        farmingItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        farmingItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 1);
        lore.clear();
        this.inventory.setItem(11, farmingItem);

        forestryItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<dark_green><bold>Forestry</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to get resources from nature."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on Hunters, Fishermen, Herbalists, etc."));
        forestryItem.lore(lore);
        forestryItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        forestryItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 2);
        lore.clear();
        this.inventory.setItem(12, forestryItem);

        miningItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Mining</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to extract minerals from the earth."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on miners or quarry workers, for example."));
        miningItem.lore(lore);
        miningItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        miningItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 3);
        lore.clear();
        this.inventory.setItem(19, miningItem);

        mistGatheringItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<aqua><bold>Mist Gathering</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with gathering Architite from the Aurosphere."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on Mist Gatherers."));
        mistGatheringItem.lore(lore);
        mistGatheringItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        mistGatheringItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 4);
        lore.clear();
        this.inventory.setItem(20, mistGatheringItem);

        woodProcessingItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<dark_green><bold>Wood Processing</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do gathering wood and processing it into other resources."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on loggers and charcoal makers."));
        woodProcessingItem.lore(lore);
        woodProcessingItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        woodProcessingItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 5);
        lore.clear();
        this.inventory.setItem(21, woodProcessingItem);


        alchemyItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<purple><bold>Alchemy</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to create potions and alchemicals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by alchemists, potion sellers, etc."));
        alchemyItem.lore(lore);
        alchemyItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        alchemyItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 6);
        lore.clear();
        this.inventory.setItem(37, alchemyItem);

        artificingItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<blue><bold>Artificing</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Artificers use Runes to create a variety of magical items."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by enchanters, artificers, etc."));
        artificingItem.lore(lore);
        artificingItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        artificingItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 7);
        lore.clear();
        this.inventory.setItem(38, artificingItem);

        cookingItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<red><bold>Cooking</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to prepare food."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by cooks, chefs, etc."));
        cookingItem.lore(lore);
        cookingItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        cookingItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 8);
        lore.clear();
        this.inventory.setItem(39, cookingItem);

        craftsmanItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Craftsman</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to craft miscellaneous items."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by carpenters, engineers, candlemakers, etc."));
        craftsmanItem.lore(lore);
        craftsmanItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        craftsmanItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 9);
        lore.clear();
        this.inventory.setItem(40, craftsmanItem);

        metalworkingItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Metalworking</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to work with metals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by blacksmiths, armorers, etc."));
        metalworkingItem.lore(lore);
        metalworkingItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        metalworkingItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 10);
        lore.clear();
        this.inventory.setItem(41, metalworkingItem);

        tailoringItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Tailoring</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to work with fabrics and hides."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by tailors, seamstresses, leatherworkers, etc."));
        tailoringItem.lore(lore);
        tailoringItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        tailoringItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 11);
        lore.clear();
        this.inventory.setItem(42, tailoringItem);

        witchcraftItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<dark_purple><bold>Witchcraft</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>The art of enchanting items or creating familiars through various rituals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by witches, some kinds of priests, etc."));
        witchcraftItem.lore(lore);
        witchcraftItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        witchcraftItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 12);
        lore.clear();
        this.inventory.setItem(43, witchcraftItem);

        economicalItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gold><bold>Economical</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to manage money and resources."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by merchants, bankers, etc."));
        economicalItem.lore(lore);
        economicalItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        economicalItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 13);
        lore.clear();
        this.inventory.setItem(14, economicalItem);

        scholarlyItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Scholarly</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's miscellaneous academical knowledge."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by sages, teachers, scribes, etc."));
        scholarlyItem.lore(lore);
        scholarlyItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        scholarlyItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 14);
        lore.clear();
        this.inventory.setItem(15, scholarlyItem);

        entertainmentItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Entertainment</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to entertain others."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by bards, jesters, etc."));
        entertainmentItem.lore(lore);
        entertainmentItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        entertainmentItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 15);
        lore.clear();
        this.inventory.setItem(16, entertainmentItem);

        martialItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Martial</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Knowledge related to military matters and fighting manuals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by soldiers, mercenaries, etc."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<red>Note: This skill doesn't give you combat abilities inherently."));
        martialItem.lore(lore);
        martialItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        martialItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 16);
        lore.clear();
        this.inventory.setItem(23, martialItem);

        medicalItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Medical</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to heal others."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by healers, doctors, etc."));
        medicalItem.lore(lore);
        medicalItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        medicalItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 17);
        lore.clear();
        this.inventory.setItem(24, medicalItem);

        tandeItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gray><bold>Transportation and Exploration</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to explore, navigate and find their way."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by scouts, navigators, etc."));
        tandeItem.lore(lore);
        tandeItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        tandeItem.getItemMeta().getPersistentDataContainer().set(skillButtonKey, PersistentDataType.INTEGER, 18);
        lore.clear();
        this.inventory.setItem(25, tandeItem);

        ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        confirmItem.getItemMeta().displayName(getPlugin().getUtils().getMiniMessage().deserialize("<green>Confirm"));
        confirmItem.getItemMeta().getPersistentDataContainer().set(selectableKey, PersistentDataType.BOOLEAN, true);
        confirmItem.getItemMeta().getPersistentDataContainer().set(optionButtonKey, PersistentDataType.STRING, "confirm");
        this.inventory.setItem(49, confirmItem);
    }

    public boolean setSelectedSkills(int skill){
       if(skillPoints == 0){
           return false;
       }
       selectedSkills[2-skillPoints] = Utils.Skills.fromInt(skill);
       skillPoints--;
       return true;
    }

    public boolean removeSelectedSkills(int skill){
        if(selectedSkills[0].getValue() == skill) {
            selectedSkills[0] = null;
            if(selectedSkills[1] != null){
                selectedSkills[0] = selectedSkills[1];
                selectedSkills[1] = null;
            }
            return true;
        } else if (selectedSkills[1].getValue() == skill){
            selectedSkills[1] = null;
            return true;
        }
        return false;
    }


    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
