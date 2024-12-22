package org.skysongdev.skysongstats.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.sql.SQLException;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class PlayerRegisterListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        boolean isOldPlayer = false;
        try{
            isOldPlayer = getPlugin().getUtils().isPlayerInDatabase(event.getPlayer().getUniqueId().toString());
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[SkysongStats] Some Error happened when looking for the player in the database!");
            e.printStackTrace();
        }
        if(!isOldPlayer){
            PlayerStats stats = new PlayerStats(event.getPlayer().getUniqueId().toString());
            getPlugin().getUtils().getStatsManager().addStatProfile(stats);
            getPlugin().getUtils().getProfileManager().createProfile(stats);
            PlayerSkills skills = new PlayerSkills(event.getPlayer().getUniqueId().toString());
            getPlugin().getUtils().getSkillManager().addSkillProfile(skills);
            getPlugin().getUtils().getProfileManager().createSetupProfile(event.getPlayer().getUniqueId().toString());
        }
        if(!getPlugin().getUtils().getProfileManager().findActiveSetupProfile(event.getPlayer().getUniqueId().toString()).isSetUp()){
            event.getPlayer().sendMessage(getPlugin().getUtils().getMiniMessage().deserialize(getPlugin().getUtils().PLUGIN_TAG + "<green>You have yet to set up your profile! Use /setup to get started!"));
        }
    }
}
