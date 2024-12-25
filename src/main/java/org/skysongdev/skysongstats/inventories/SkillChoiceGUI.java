package org.skysongdev.skysongstats.inventories;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class SkillChoiceGUI implements InventoryHolder {
    Inventory inventory;
    SetupProfile setupProfile;

    Player targetPlayer;

    ItemStack animalHandlingItem = new ItemStack(Material.BONE);
    ItemStack farmingItem = new ItemStack(Material.WHEAT);
    ItemStack forestryItem = new ItemStack(Material.FERN);
    ItemStack miningItem = new ItemStack(Material.IRON_ORE);
    ItemStack mistGatheringItem = new ItemStack(Material.GLOWSTONE_DUST);
    ItemStack woodProcessingItem = new ItemStack(Material.OAK_LOG);
    ItemStack alchemyItem = new ItemStack(Material.BREWING_STAND);
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

    ItemStack headerItem = new ItemStack(Material.NETHER_STAR);

    public SkillChoiceGUI(SetupProfile setupProfile) {
        this.inventory = getPlugin().getServer().createInventory(this, 54);
        this.setupProfile = setupProfile;
    }

    public void setupInventory(Player player){
        targetPlayer = player;
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        List<Component> lore = new ArrayList<>();
        ItemMeta bufferMeta;
        PersistentDataContainer bufferContainer;
        for(int i = 0; i < 9; i++){
            this.inventory.setItem(i, borderItem);
        }
        for(int i = 45; i < 54; i++){
            this.inventory.setItem(i, borderItem);
        }
        bufferMeta = animalHandlingItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#45c2ea><bold>Animal Handling</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to interact with animals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on animal trainers, animal farmers, or horse breeders, for example."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 0);
        animalHandlingItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(10, animalHandlingItem);

        bufferMeta = farmingItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#f9f36d><bold>Farming</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to tend to crops and plants."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on farmers, gardeners or orchard farmers, for example."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 1);
        farmingItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(11, farmingItem);

        bufferMeta = forestryItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#54bf3e><bold>Forestry</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to get resources from nature."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on Hunters, Fishermen, Herbalists, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 2);
        forestryItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(12, forestryItem);

        bufferMeta = miningItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#a0836f><bold>Mining</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to extract minerals from the earth."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on miners or quarry workers, for example."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 3);
        miningItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(19, miningItem);

        bufferMeta = mistGatheringItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gold><bold>Mist Gathering</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with gathering Architite from the Aurosphere."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on Mist Gatherers."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 4);
        mistGatheringItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(20, mistGatheringItem);

        bufferMeta = woodProcessingItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#ba7f3d><bold>Wood Processing</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do gathering wood and processing it into other resources."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen on loggers and charcoal makers."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 5);
        woodProcessingItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(21, woodProcessingItem);


        bufferMeta = alchemyItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#aaeb38><bold>Alchemy</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to create potions and alchemicals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by alchemists, potion sellers, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 6);
        alchemyItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(37, alchemyItem);

        bufferMeta = artificingItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#2cf0f1><bold>Artificing</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Artificers use Runes to create a variety of magical items."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by enchanters, artificers, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 7);
        artificingItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(38, artificingItem);

        bufferMeta = cookingItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#f77b3c><bold>Cooking</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to prepare food."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by cooks, chefs, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 8);
        cookingItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(39, cookingItem);

        bufferMeta = craftsmanItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#eba751><bold>Craftsman</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to craft miscellaneous items."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by carpenters, engineers, candlemakers, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 9);
        craftsmanItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(40, craftsmanItem);

        bufferMeta = metalworkingItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#8d8d8d><bold>Metalworking</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to work with metals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by blacksmiths, armorers, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 10);
        metalworkingItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(41, metalworkingItem);

        bufferMeta = tailoringItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#ea5be7><bold>Tailoring</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to work with fabrics and hides."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by tailors, seamstresses, leatherworkers, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 11);
        tailoringItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(42, tailoringItem);

        bufferMeta = witchcraftItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#8235b0><bold>Witchcraft</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>The art of enchanting items or creating familiars through various rituals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by witches, some kinds of priests, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 12);
        witchcraftItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(43, witchcraftItem);

        bufferMeta = economicalItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gold><bold>Economical</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to manage money and resources."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by merchants, bankers, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 13);
        economicalItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(14, economicalItem);

        bufferMeta = scholarlyItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#5375e9><bold>Scholarly</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's miscellaneous academical knowledge."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by sages, teachers, scribes, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 14);
        scholarlyItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(15, scholarlyItem);

        bufferMeta = entertainmentItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#f22a93><bold>Entertainment</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to entertain others."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by bards, jesters, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 15);
        entertainmentItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(16, entertainmentItem);

        bufferMeta = martialItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#e18048><bold>Martial</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Knowledge related to military matters and fighting manuals."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by soldiers, mercenaries, etc."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<red>Note: This skill doesn't give you combat abilities inherently."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 16);
        martialItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(23, martialItem);

        bufferMeta = medicalItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#f14f4f><bold>Medical</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to heal others."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by healers, doctors, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 17);
        medicalItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(24, medicalItem);

        bufferMeta = tandeItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<#7fe95b><bold>Transportation and Exploration</bold>"));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>This skill has to do with your character's ability to explore, navigate and find their way."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>It's often seen utilized by scouts, navigators, etc."));
        bufferMeta.lore(lore);
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(skillButtonKey, PersistentDataType.INTEGER, 18);
        tandeItem.setItemMeta(bufferMeta);
        lore.clear();
        this.inventory.setItem(25, tandeItem);

        ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        bufferMeta = confirmItem.getItemMeta();
        bufferMeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<green>Confirm"));
        bufferContainer = bufferMeta.getPersistentDataContainer();
        bufferContainer.set(selectableKey, PersistentDataType.BOOLEAN, true);
        bufferContainer.set(optionButtonKey, PersistentDataType.STRING, "confirm");
        confirmItem.setItemMeta(bufferMeta);
        this.inventory.setItem(49, confirmItem);

        updateHeader();
    }

    public void updateHeader(){
        ItemMeta meta = headerItem.getItemMeta();
        meta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gold><bold>Skills Selection"));
        List<Component> lore = new ArrayList<>();
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Select two skills that you want to specialize in."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>You can't change them later."));
        lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>You have " + setupProfile.skillPoints + " skills left to choose."));
        meta.lore(lore);
        headerItem.setItemMeta(meta);
        this.inventory.setItem(4, headerItem);
    }

    public boolean setSelectedSkills(int skill){
       if(setupProfile.skillPoints <= 0){
           return false;
       }
       if(setupProfile.skillsSelected.contains(skill)){
           return false;
       }
        setupProfile.skillsSelected.add(skill);
        setupProfile.skillPoints--;
       return true;
    }

    public boolean removeSelectedSkills(int skill) {
        for (int i = 0; i < setupProfile.skillsSelected.size(); i++) {
            if (setupProfile.skillsSelected.get(i) == skill) {
                setupProfile.skillsSelected.remove(i);
                setupProfile.skillPoints++;
                return true;
            }
        }
        return false;
    }

    public void resetSelectedSkills(){
        setupProfile.skillsSelected.clear();
        setupProfile.skillPoints = 2;
    }


    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
