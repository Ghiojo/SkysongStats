package org.skysongdev.skysongstats.listeners;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.skysongdev.skysongstats.Utils.SkysongItemsKeys;
import org.skysongdev.skysongstats.database.PlayerStats;

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

    static int CountArcrot(Player player){
        return CountArcrot(player.getInventory().getContents());
    }

    static int CountArcrot(ItemStack[] items){
        int arcrotCount = 0;

        for(ItemStack item : items){
            if(item == null) continue;
            if(item.getType() == Material.AIR) continue;
            PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
            if(data.has(SkysongItemsKeys.ARCROT)){
                arcrotCount += data.get(SkysongItemsKeys.ARCROT, PersistentDataType.INTEGER) * item.getAmount();
            }
            // Charged arcrot only counts while the item has charges left.
            if(data.has(SkysongItemsKeys.CHARGED_ARCROT) && data.has(SkysongItemsKeys.CHARGES)
                    && data.get(SkysongItemsKeys.CHARGES, PersistentDataType.INTEGER) > 0){
                arcrotCount += data.get(SkysongItemsKeys.CHARGED_ARCROT, PersistentDataType.INTEGER) * item.getAmount();
            }
        }

        return arcrotCount;
    }

    static int CountFictis(Player player){
        return CountFictis(player.getInventory().getContents());
    }

    static int CountFictis(ItemStack[] items){
        int fictisCount = 0;
        for(ItemStack item : items){
            if(item == null) continue;
            if(item.getType() == Material.AIR) continue;
            PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
            if(data.has(SkysongItemsKeys.FICTIS)){
                fictisCount += data.get(SkysongItemsKeys.FICTIS, PersistentDataType.INTEGER) * item.getAmount();
            }
        }
        return fictisCount;
    }
}
