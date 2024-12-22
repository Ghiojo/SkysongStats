package org.skysongdev.skysongstats.Utils;

import org.bukkit.Bukkit;
import org.skysongdev.skysongstats.database.Modifier;
import org.skysongdev.skysongstats.database.PlayerStats;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class StatsManager {
    public static ArrayList<PlayerStats> statsProfileList;

    public PlayerStats findStats(String uuid, String profile){
        for(PlayerStats current : StatsManager.statsProfileList){
            if(Objects.equals(current.getUuid(), uuid) && Objects.equals(current.getProfile(), profile)){
                return current;
            }
        }
        return null;
    }
    public void addStatProfile(PlayerStats stat){
        try {
            getPlugin().getDatabase().createStatData(stat);
            StatsManager.statsProfileList.add(stat);
        }catch (SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to add a stat profile!");
            e.printStackTrace();
        }
    }
    public void addModifier(PlayerStats stats, Modifier modifier){
        stats.addModifier(modifier);
        try{
            getPlugin().getDatabase().createModifierData(stats, stats.getModifierIndex(modifier));
        } catch(SQLException e){
            Bukkit.getLogger().warning("[SkysongStats] Unable to add a modifier!");
            e.printStackTrace();
        }
    }
    public void updateStats(PlayerStats stat){
        try {
            getPlugin().getDatabase().updateStatData(stat);
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongStats] Couldn't update stats!");
            e.printStackTrace();
        }
    }
    public void deleteStat(PlayerStats stat){
        try {
            getPlugin().getDatabase().deleteStatData(stat);
            StatsManager.statsProfileList.remove(stat);
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongStats] Couldn't delete stat data!");
            e.printStackTrace();
        }
    }
    public void deleteModifiers(PlayerStats stat){
        try {
            for(Modifier modifier : stat.getModifiers()){
                getPlugin().getDatabase().deleteModifierData(stat, stat.getModifierIndex(modifier));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongStats] Couldn't delete modifiers!");
            e.printStackTrace();
        }
    }
}
