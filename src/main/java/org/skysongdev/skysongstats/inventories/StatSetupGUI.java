package org.skysongdev.skysongstats.inventories;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.*;

public class StatSetupGUI implements InventoryHolder {
    private final Inventory inventory;
    private List<String> set;

    public StatSetupGUI() {
        this.inventory = getPlugin().getServer().createInventory(this, 45);
        this.set = getPlugin().getConfig().getConfigurationSection("base_stats").getKeys(false).stream().toList();
        setupInventory();
    }

    public void setupInventory(){
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for(int i = 0; i < 9; i++){
            this.inventory.setItem(i, borderItem);
        }
        for(int i = 36; i < 45; i++){
            this.inventory.setItem(i, borderItem);
        }
        ItemStack descriptionItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta descmeta = descriptionItem.getItemMeta();
        descmeta.displayName(getPlugin().getUtils().getMiniMessage().deserialize("<gold>Base Statline Selection"));
        List<Component> desclore = new ArrayList<>();
        desclore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>Select whichever base statline you believe fits your"));
        desclore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>character the best! Remember that your base stats don't have"));
        desclore.add(getPlugin().getUtils().getMiniMessage().deserialize("<gray>to match the ancestry you are picking!"));
        descmeta.lore(desclore);
        descriptionItem.setItemMeta(descmeta);
        this.inventory.setItem(4, descriptionItem);

        for(int i = 0; i < set.size() && i < 27; i++){
            Material mat = Material.getMaterial(getPlugin().getConfig().getString("base_stats." + set.get(i) + ".item"));
            if(mat == null){
                mat = Material.BARRIER;
            }
            ItemStack statlineItem = new ItemStack(mat);
            ItemMeta meta = statlineItem.getItemMeta();
            meta.displayName(getPlugin().getUtils().getMiniMessage().deserialize(SkysongStats.getPlugin().getConfig().getString("base_stats." + set.get(i) + ".name")));
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(selectableKey, PersistentDataType.BOOLEAN, true);
            container.set(statlineKey, PersistentDataType.STRING, set.get(i));
            statlineItem.setItemMeta(meta);
            List<Component> lore = new ArrayList<>();
            lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<red>Strength: " + getPlugin().getConfig().getInt("base_stats." + set.get(i) + ".stats.strength")));
            lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<green>Dexterity: " + getPlugin().getConfig().getInt("base_stats." + set.get(i) + ".stats.dexterity")));
            lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<light_purple>Constitution: " + getPlugin().getConfig().getInt("base_stats." + set.get(i) + ".stats.constitution")));
            lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<blue>Focus: " + getPlugin().getConfig().getInt("base_stats." + set.get(i) + ".stats.focus")));
            lore.add(getPlugin().getUtils().getMiniMessage().deserialize("<yellow>Speed: " + getPlugin().getConfig().getInt("base_stats." + set.get(i) + ".stats.speed")));
            statlineItem.lore(lore);
            this.inventory.setItem(i+9, statlineItem);
        }
    }


    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
