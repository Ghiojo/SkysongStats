package org.skysongdev.skysongstats.listeners;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongitems.utils.Utils;
import org.skysongdev.skysongstats.database.PlayerStats;

import java.util.ArrayList;
import java.util.Arrays;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class AfflictionsListener implements Listener {
    @EventHandler
    public void onInventoryChange(PlayerInventorySlotChangeEvent e){
        Player player = e.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        PlayerStats stats = getPlugin().getUtils().getProfileManager().findActiveStats(playerUUID);

        stats.setArcrot(CountArcrot(player));
        stats.setFictis(CountFictis(player));

        getPlugin().getUtils().getStatsManager().updateStats(stats);

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        PlayerStats stats = getPlugin().getUtils().getProfileManager().findActiveStats(playerUUID);

        stats.setArcrot(CountArcrot(player));
        stats.setFictis(CountFictis(player));

        getPlugin().getUtils().getStatsManager().updateStats(stats);
    }

    private int CountArcrot(Player player){
        Inventory inv = player.getInventory();
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.stream(inv.getContents()).toList());
        int arcrotCount = 0;
        int buffer = 0;

        for(ItemStack item : items){
            if(item == null) continue;
            if(item.getType() == Material.AIR) continue;
            if(!item.getItemMeta().getPersistentDataContainer().has(Utils.getTag("arcrot")) && !item.getItemMeta().getPersistentDataContainer().has(Utils.getTag("charged_arcrot")))
                continue;
            if(item.getItemMeta().getPersistentDataContainer().has(Utils.getTag("arcrot"))){
                buffer = item.getItemMeta().getPersistentDataContainer().get(Utils.getTag("arcrot"), PersistentDataType.INTEGER);
            }
            arcrotCount += buffer * item.getAmount();
            buffer = 0;
            if(item.getItemMeta().getPersistentDataContainer().has(Utils.getTag("charged_arcrot"))){
                if(item.getItemMeta().getPersistentDataContainer().has(Utils.chargesKey))
                    if(item.getItemMeta().getPersistentDataContainer().get(Utils.chargesKey, PersistentDataType.INTEGER) > 0)
                        buffer = item.getItemMeta().getPersistentDataContainer().get(Utils.getTag("charged_arcrot"), PersistentDataType.INTEGER);
            }
            arcrotCount += buffer * item.getAmount();
            buffer = 0;
        }

        return arcrotCount;
    }

    private int CountFictis(Player player){
        Inventory inv = player.getInventory();
        ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.stream(inv.getContents()).toList());
        int fictisCount = 0;
        int buffer = 0;
        for(ItemStack item : items){
            if(item == null) continue;
            if(item.getType() == Material.AIR) continue;
            if(!item.getItemMeta().getPersistentDataContainer().has(Utils.getTag("fictis")))
                continue;
            if(item.getItemMeta().getPersistentDataContainer().has(Utils.getTag("fictis"))){
                buffer = item.getItemMeta().getPersistentDataContainer().get(Utils.getTag("fictis"), PersistentDataType.INTEGER);
            }
            fictisCount += buffer * item.getAmount();
            buffer = 0;
        }
        return fictisCount;
    }
}
